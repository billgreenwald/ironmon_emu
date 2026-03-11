package org.libsdl.app

import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.Sensor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.PointerIcon
import android.view.Surface
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.libsdl.app.HIDDeviceManager.Companion.acquire
import org.libsdl.app.HIDDeviceManager.Companion.release
import org.libsdl.app.SDL.getContext
import org.libsdl.app.SDL.loadLibrary
import org.libsdl.app.SDL.setContext
import org.libsdl.app.SDL.setupJNI
import org.libsdl.app.SDLControllerManager.isDeviceSDLJoystick
import org.libsdl.app.SDLControllerManager.onNativePadDown
import org.libsdl.app.SDLControllerManager.onNativePadUp
import org.libsdl.app.SDLInputConnection.Companion.nativeCommitText
import java.util.Hashtable
import java.util.Locale
import java.util.Objects

/**
 * SDL Activity
 */
open class SDLActivity : AppCompatActivity(), OnSystemUiVisibilityChangeListener {
    // Handle the state of the native layer
    enum class NativeState {
        INIT,
        RESUMED,
        PAUSED
    }

    val mainSharedObject: String
        /**
         * This method returns the name of the shared object with the application entry point
         * It can be overridden by derived classes.
         */
        get() {
            val library: String
            val libraries = mSingleton!!.libraries
            library = if (libraries.size > 0) {
                "lib" + libraries[libraries.size - 1] + ".so"
            } else {
                "libmain.so"
            }
            return context!!.applicationInfo.nativeLibraryDir + "/" + library
        }
    val mainFunction: String
        /**
         * This method returns the name of the application entry point
         * It can be overridden by derived classes.
         */
        get() = "SDL_main"
    protected open val libraries: Array<String>
        /**
         * This method is called by SDL before loading the native shared libraries.
         * It can be overridden to provide names of shared libraries to be loaded.
         * The default implementation returns the defaults. It never returns null.
         * An array returned by a new implementation must at least contain "SDL2".
         * Also keep in mind that the order the libraries are loaded may matter.
         * @return names of shared libraries to be loaded (e.g. "SDL2", "main").
         */
        protected get() = arrayOf(
            "SDL2",  // "SDL2_image",
            // "SDL2_mixer",
            // "SDL2_net",
            // "SDL2_ttf",
            "main"
        )

    // Load the .so
    fun loadLibraries() {
        for (lib in libraries) {
            loadLibrary(lib)
        }
    }

    open fun getArguments(): Array<String> {
        return arrayOf()
    }

    protected fun createSDLSurface(context: Context): SDLSurface {
        return SDLSurface(context)
    }

    // Setup
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "Device: " + Build.DEVICE)
        Log.v(TAG, "Model: " + Build.MODEL)
        Log.v(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        try {
            Thread.currentThread().name = "SDLActivity"
        } catch (e: Exception) {
            Log.v(TAG, "modify thread properties failed $e")
        }

        // Load shared libraries
        var errorMsgBrokenLib = ""
        try {
            loadLibraries()
            mBrokenLibraries = false /* success */
        } catch (e: UnsatisfiedLinkError) {
            System.err.println(e.message)
            mBrokenLibraries = true
            errorMsgBrokenLib = e.message ?: ""
        } catch (e: Exception) {
            System.err.println(e.message)
            mBrokenLibraries = true
            errorMsgBrokenLib = e.message ?: ""
        }
        if (!mBrokenLibraries) {
            val expected_version =
                SDL_MAJOR_VERSION.toString() + "." + SDL_MINOR_VERSION.toString() + "." + SDL_MICRO_VERSION.toString()
            val version = nativeGetVersion()
            if (version != expected_version) {
                mBrokenLibraries = true
                errorMsgBrokenLib =
                    "SDL C/Java version mismatch (expected $expected_version, got $version)"
            }
        }
        if (mBrokenLibraries) {
            mSingleton = this
            val dlgAlert = AlertDialog.Builder(this)
            dlgAlert.setMessage(
                "An error occurred while trying to start the application. Please try again and/or reinstall."
                        + System.getProperty("line.separator")
                        + System.getProperty("line.separator")
                        + "Error: " + errorMsgBrokenLib
            )
            dlgAlert.setTitle("SDL Error")
            dlgAlert.setPositiveButton(
                "Exit"
            ) { dialog, id -> // if this button is clicked, close current activity
                mSingleton!!.finish()
            }
            dlgAlert.setCancelable(false)
            dlgAlert.create().show()
            return
        }

        // Set up JNI
        setupJNI()

        // Initialize state
        SDL.initialize()

        // So we can call stuff from static callbacks
        mSingleton = this
        setContext(this)
        mClipboardHandler = SDLClipboardHandler()
        mHIDDeviceManager = acquire(this)

        // Set up the surface
        mSurface = createSDLSurface(application)
        mLayout = RelativeLayout(this)
        mLayout?.addView(mSurface)

        // Get our current screen orientation and pass it down.
        mCurrentOrientation = currentOrientation
        // Only record current orientation
        onNativeOrientationChanged(mCurrentOrientation)
        try {
            if (Build.VERSION.SDK_INT < 24) {
                mCurrentLocale = context!!.resources.configuration.locale
            } else {
                mCurrentLocale = context!!.resources.configuration.locales[0]
            }
        } catch (ignored: Exception) {
        }
        setContentView(mLayout)
        setWindowStyle(false)
        window.decorView.setOnSystemUiVisibilityChangeListener(this)

        // Get filename from "Open with" of another application
        val intent = intent
        if (intent != null && intent.data != null) {
            val filename = intent.data!!.path
            if (filename != null) {
                Log.v(TAG, "Got filename: $filename")
                onNativeDropFile(filename)
            }
        }
    }

    protected fun pauseNativeThread() {
        mNextNativeState = NativeState.PAUSED
        mIsResumedCalled = false
        if (mBrokenLibraries) {
            return
        }
        handleNativeState()
    }

    protected open fun resumeNativeThread() {
        mNextNativeState = NativeState.RESUMED
        mIsResumedCalled = true
        if (mBrokenLibraries) {
            return
        }
        handleNativeState()
    }

    // Events
    override fun onPause() {
        Log.v(TAG, "onPause()")
        super.onPause()
        if (mHIDDeviceManager != null) {
            mHIDDeviceManager!!.setFrozen(true)
        }
        if (!mHasMultiWindow) {
            pauseNativeThread()
        }
    }

    override fun onResume() {
        Log.v(TAG, "onResume()")
        super.onResume()
        if (mHIDDeviceManager != null) {
            mHIDDeviceManager!!.setFrozen(false)
        }
        if (!mHasMultiWindow) {
            resumeNativeThread()
        }
    }

    override fun onStop() {
        Log.v(TAG, "onStop()")
        super.onStop()
        if (mHasMultiWindow) {
            pauseNativeThread()
        }
    }

    override fun onStart() {
        Log.v(TAG, "onStart()")
        super.onStart()
        if (mHasMultiWindow) {
            resumeNativeThread()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.v(TAG, "onWindowFocusChanged(): $hasFocus")
        if (mBrokenLibraries) {
            return
        }
        mHasFocus = hasFocus
        if (hasFocus) {
            mNextNativeState = NativeState.RESUMED
            motionListener!!.reclaimRelativeMouseModeIfNeeded()
            handleNativeState()
            nativeFocusChanged(true)
        } else {
            nativeFocusChanged(false)
            if (!mHasMultiWindow) {
                mNextNativeState = NativeState.PAUSED
                handleNativeState()
            }
        }
    }

    override fun onLowMemory() {
        Log.v(TAG, "onLowMemory()")
        super.onLowMemory()
        if (mBrokenLibraries) {
            return
        }
        nativeLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.v(TAG, "onConfigurationChanged()")
        super.onConfigurationChanged(newConfig)
        if (mBrokenLibraries) {
            return
        }
        if (mCurrentLocale == null || mCurrentLocale != newConfig.locale) {
            mCurrentLocale = newConfig.locale
            onNativeLocaleChanged()
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy()")
        if (mHIDDeviceManager != null) {
            release(mHIDDeviceManager!!)
            mHIDDeviceManager = null
        }
        if (mBrokenLibraries) {
            super.onDestroy()
            return
        }
        if (mSDLThread != null) {

            // Send Quit event to "SDLThread" thread
            nativeSendQuit()

            // Wait for "SDLThread" thread to end
            try {
                mSDLThread!!.join()
            } catch (e: Exception) {
                Log.v(TAG, "Problem stopping SDLThread: $e")
            }
        }
        nativeQuit()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Check if we want to block the back button in case of mouse right click.
        //
        // If we do, the normal hardware back button will no longer work and people have to use home,
        // but the mouse right click will work.
        //
        val trapBack = nativeGetHintBoolean("SDL_ANDROID_TRAP_BACK_BUTTON", false)
        if (trapBack) {
            // Exit and let the mouse handler handle this button (if appropriate)
            return
        }

        // Default system back button behavior.
        if (!isFinishing) {
            super.onBackPressed()
        }
    }

    // Used to get us onto the activity's main thread
    fun pressBackButton() {
        runOnUiThread {
            if (!this@SDLActivity.isFinishing) {
                superOnBackPressed()
            }
        }
    }

    // Used to access the system back behavior.
    fun superOnBackPressed() {
        super.onBackPressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mBrokenLibraries) {
            return false
        }
        val keyCode = event.keyCode
        // Ignore certain special keys so they're handled by Android
        return if (/* API 11 */keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_ZOOM_IN || keyCode == KeyEvent.KEYCODE_ZOOM_OUT
        ) {
            false
        } else super.dispatchKeyEvent(event)
    }

    /**
     * This method is called by SDL if SDL did not handle a message itself.
     * This happens if a received message contains an unsupported command.
     * Method can be overwritten to handle Messages in a different class.
     * @param command the command of the message.
     * @param param the parameter of the message. May be null.
     * @return if the message was handled in overridden method.
     */
    protected fun onUnhandledMessage(command: Int, param: Any?): Boolean {
        return false
    }

    /**
     * A Handler class for Messages from native SDL applications.
     * It uses current Activities as target (e.g. for the title).
     * static to prevent implicit references to enclosing object.
     */
    protected class SDLCommandHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val context = getContext()
            if (context == null) {
                Log.e(TAG, "error handling message, getContext() returned null")
                return
            }
            when (msg.arg1) {
                COMMAND_CHANGE_TITLE -> if (context is Activity) {
                    context.title = msg.obj as String
                } else {
                    Log.e(TAG, "error handling message, getContext() returned no Activity")
                }

                COMMAND_CHANGE_WINDOW_STYLE -> if (Build.VERSION.SDK_INT >= 19) {
                    if (context is Activity) {
                        val window = context.window
                        if (window != null) {
                            if (msg.obj is Int && msg.obj as Int != 0) {
                                val flags = View.SYSTEM_UI_FLAG_FULLSCREEN or
                                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.INVISIBLE
                                window.decorView.systemUiVisibility = flags
                                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                                mFullscreenModeActive = true
                            } else {
                                val flags =
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_VISIBLE
                                window.decorView.systemUiVisibility = flags
                                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                                mFullscreenModeActive = false
                            }
                        }
                    } else {
                        Log.e(TAG, "error handling message, getContext() returned no Activity")
                    }
                }

                COMMAND_TEXTEDIT_HIDE -> if (mTextEdit != null) {
                    // Note: On some devices setting view to GONE creates a flicker in landscape.
                    // Setting the View's sizes to 0 is similar to GONE but without the flicker.
                    // The sizes will be set to useful values when the keyboard is shown again.
                    mTextEdit!!.layoutParams = RelativeLayout.LayoutParams(0, 0)
                    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(mTextEdit!!.windowToken, 0)
                    mScreenKeyboardShown = false
                    mSurface!!.requestFocus()
                }

                COMMAND_SET_KEEP_SCREEN_ON -> {
                    if (context is Activity) {
                        val window = context.window
                        if (window != null) {
                            if (msg.obj is Int && msg.obj as Int != 0) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }
                }

                else -> if (context is SDLActivity && !context.onUnhandledMessage(
                        msg.arg1,
                        msg.obj
                    )
                ) {
                    Log.e(TAG, "error handling message, command is " + msg.arg1)
                }
            }
        }
    }

    // Handler for the messages
    var commandHandler: Handler = SDLCommandHandler()

    // Send a message from the SDLMain thread
    fun sendCommand(command: Int, data: Any?): Boolean {
        val msg = commandHandler.obtainMessage()
        msg.arg1 = command
        msg.obj = data
        val result = commandHandler.sendMessage(msg)
        if (Build.VERSION.SDK_INT >= 19) {
            if (command == COMMAND_CHANGE_WINDOW_STYLE) {
                // Ensure we don't return until the resize has actually happened,
                // or 500ms have passed.
                var bShouldWait = false
                if (data is Int) {
                    // Let's figure out if we're already laid out fullscreen or not.
                    val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
                    val realMetrics = DisplayMetrics()
                    display.getRealMetrics(realMetrics)
                    val bFullscreenLayout =
                        realMetrics.widthPixels == mSurface!!.width && realMetrics.heightPixels == mSurface!!.height
                    bShouldWait = if (data == 1) {
                        // If we aren't laid out fullscreen or actively in fullscreen mode already, we're going
                        // to change size and should wait for surfaceChanged() before we return, so the size
                        // is right back in native code.  If we're already laid out fullscreen, though, we're
                        // not going to change size even if we change decor modes, so we shouldn't wait for
                        // surfaceChanged() -- which may not even happen -- and should return immediately.
                        !bFullscreenLayout
                    } else {
                        // If we're laid out fullscreen (even if the status bar and nav bar are present),
                        // or are actively in fullscreen, we're going to change size and should wait for
                        // surfaceChanged before we return, so the size is right back in native code.
                        bFullscreenLayout
                    }
                }
                if (bShouldWait && context != null) {
                    // We'll wait for the surfaceChanged() method, which will notify us
                    // when called.  That way, we know our current size is really the
                    // size we need, instead of grabbing a size that's still got
                    // the navigation and/or status bars before they're hidden.
                    //
                    // We'll wait for up to half a second, because some devices
                    // take a surprisingly long time for the surface resize, but
                    // then we'll just give up and return.
                    //
                    synchronized(context!!) {
                        try {
                            (context as Object).wait(500)
                        } catch (ie: InterruptedException) {
                            ie.printStackTrace()
                        }
                    }
                }
            }
        }
        return result
    }

    /**
     * This can be overridden
     */
    fun setOrientationBis(w: Int, h: Int, resizable: Boolean, hint: String) {
        var orientation_landscape = -1
        var orientation_portrait = -1

        /* If set, hint "explicitly controls which UI orientations are allowed". */if (hint.contains(
                "LandscapeRight"
            ) && hint.contains("LandscapeLeft")
        ) {
            orientation_landscape = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (hint.contains("LandscapeRight")) {
            orientation_landscape = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (hint.contains("LandscapeLeft")) {
            orientation_landscape = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
        if (hint.contains("Portrait") && hint.contains("PortraitUpsideDown")) {
            orientation_portrait = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else if (hint.contains("Portrait")) {
            orientation_portrait = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (hint.contains("PortraitUpsideDown")) {
            orientation_portrait = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
        val is_landscape_allowed = orientation_landscape != -1
        val is_portrait_allowed = orientation_portrait != -1
        val req: Int /* Requested orientation */

        /* No valid hint, nothing is explicitly allowed */req =
            if (!is_portrait_allowed && !is_landscape_allowed) {
                if (resizable) {
                    /* All orientations are allowed */
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                } else {
                    /* Fixed window and nothing specified. Get orientation from w/h of created window */
                    if (w > h) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            } else {
                /* At least one orientation is allowed */
                if (resizable) {
                    if (is_portrait_allowed && is_landscape_allowed) {
                        /* hint allows both landscape and portrait, promote to full sensor */
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    } else {
                        /* Use the only one allowed "orientation" */
                        if (is_landscape_allowed) orientation_landscape else orientation_portrait
                    }
                } else {
                    /* Fixed window and both orientations are allowed. Choose one. */
                    if (is_portrait_allowed && is_landscape_allowed) {
                        if (w > h) orientation_landscape else orientation_portrait
                    } else {
                        /* Use the only one allowed "orientation" */
                        if (is_landscape_allowed) orientation_landscape else orientation_portrait
                    }
                }
            }
        Log.v(
            TAG,
            "setOrientation() requestedOrientation=$req width=$w height=$h resizable=$resizable hint=$hint"
        )
        mSingleton!!.requestedOrientation = req
    }

    internal class ShowTextInputTask(var x: Int, var y: Int, var w: Int, var h: Int) : Runnable {
        init {

            /* Minimum size of 1 pixel, so it takes focus. */if (w <= 0) {
                w = 1
            }
            if (h + HEIGHT_PADDING <= 0) {
                h = 1 - HEIGHT_PADDING
            }
        }

        override fun run() {
            val params = RelativeLayout.LayoutParams(w, h + HEIGHT_PADDING)
            params.leftMargin = x
            params.topMargin = y
            if (mTextEdit == null) {
                mTextEdit = DummyEdit(getContext())
                mLayout!!.addView(mTextEdit, params)
            } else {
                mTextEdit!!.layoutParams = params
            }
            mTextEdit!!.visibility = View.VISIBLE
            mTextEdit!!.requestFocus()
            val imm = getContext()!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(mTextEdit, 0)
            mScreenKeyboardShown = true
        }

        companion object {
            /*
         * This is used to regulate the pan&scan method to have some offset from
         * the bottom edge of the input region and the top edge of an input
         * method (soft keyboard)
         */
            const val HEIGHT_PADDING = 15
        }
    }
    // Messagebox
    /** Result of current messagebox. Also used for blocking the calling thread.  */
    protected val messageboxSelection = IntArray(1)

    /**
     * This method is called by SDL using JNI.
     * Shows the messagebox from UI thread and block calling thread.
     * buttonFlags, buttonIds and buttonTexts must have same length.
     * @param buttonFlags array containing flags for every button.
     * @param buttonIds array containing id for every button.
     * @param buttonTexts array containing text for every button.
     * @param colors null for default or array of length 5 containing colors.
     * @return button id or -1.
     */
    fun messageboxShowMessageBox(
        flags: Int,
        title: String?,
        message: String?,
        buttonFlags: IntArray,
        buttonIds: IntArray,
        buttonTexts: Array<String?>,
        colors: IntArray?
    ): Int {
        messageboxSelection[0] = -1

        // sanity checks
        if (buttonFlags.size != buttonIds.size && buttonIds.size != buttonTexts.size) {
            return -1 // implementation broken
        }

        // collect arguments for Dialog
        val args = Bundle()
        args.putInt("flags", flags)
        args.putString("title", title)
        args.putString("message", message)
        args.putIntArray("buttonFlags", buttonFlags)
        args.putIntArray("buttonIds", buttonIds)
        args.putStringArray("buttonTexts", buttonTexts)
        args.putIntArray("colors", colors)

        // trigger Dialog creation on UI thread
        runOnUiThread { messageboxCreateAndShow(args) }

        // block the calling thread
        synchronized(messageboxSelection) {
            try {
                (context as Object).wait()
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
                return -1
            }
        }

        // return selected value
        return messageboxSelection[0]
    }

    protected fun messageboxCreateAndShow(args: Bundle) {

        // TODO set values from "flags" to messagebox dialog

        // get colors
        val colors = args.getIntArray("colors")
        val backgroundColor: Int
        val textColor: Int
        val buttonBorderColor: Int
        val buttonBackgroundColor: Int
        val buttonSelectedColor: Int
        if (colors != null) {
            var i = -1
            backgroundColor = colors[++i]
            textColor = colors[++i]
            buttonBorderColor = colors[++i]
            buttonBackgroundColor = colors[++i]
            buttonSelectedColor = colors[++i]
        } else {
            backgroundColor = Color.TRANSPARENT
            textColor = Color.TRANSPARENT
            buttonBorderColor = Color.TRANSPARENT
            buttonBackgroundColor = Color.TRANSPARENT
            buttonSelectedColor = Color.TRANSPARENT
        }

        // create dialog with title and a listener to wake up calling thread
        val dialog = AlertDialog.Builder(this).create()
        dialog.setTitle(args.getString("title"))
        dialog.setCancelable(false)
        dialog.setOnDismissListener { synchronized(messageboxSelection) { (messageboxSelection as Object).notify() } }

        // create text
        val message = TextView(this)
        message.gravity = Gravity.CENTER
        message.text = args.getString("message")
        if (textColor != Color.TRANSPARENT) {
            message.setTextColor(textColor)
        }

        // create buttons
        val buttonFlags = args.getIntArray("buttonFlags")
        val buttonIds = args.getIntArray("buttonIds")
        val buttonTexts = args.getStringArray("buttonTexts")
        val mapping = SparseArray<Button>()
        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL
        buttons.gravity = Gravity.CENTER
        for (i in buttonTexts!!.indices) {
            val button = Button(this)
            val id = buttonIds!![i]
            button.setOnClickListener {
                messageboxSelection[0] = id
                dialog.dismiss()
            }
            if (buttonFlags!![i] != 0) {
                // see SDL_messagebox.h
                if (buttonFlags[i] and 0x00000001 != 0) {
                    mapping.put(KeyEvent.KEYCODE_ENTER, button)
                }
                if (buttonFlags[i] and 0x00000002 != 0) {
                    mapping.put(KeyEvent.KEYCODE_ESCAPE, button) /* API 11 */
                }
            }
            button.text = buttonTexts[i]
            if (textColor != Color.TRANSPARENT) {
                button.setTextColor(textColor)
            }
            if (buttonBorderColor != Color.TRANSPARENT) {
                // TODO set color for border of messagebox button
            }
            if (buttonBackgroundColor != Color.TRANSPARENT) {
                val drawable = button.background
                if (drawable == null) {
                    // setting the color this way removes the style
                    button.setBackgroundColor(buttonBackgroundColor)
                } else {
                    // setting the color this way keeps the style (gradient, padding, etc.)
                    drawable.setColorFilter(buttonBackgroundColor, PorterDuff.Mode.MULTIPLY)
                }
            }
            if (buttonSelectedColor != Color.TRANSPARENT) {
                // TODO set color for selected messagebox button
            }
            buttons.addView(button)
        }

        // create content
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.addView(message)
        content.addView(buttons)
        if (backgroundColor != Color.TRANSPARENT) {
            content.setBackgroundColor(backgroundColor)
        }

        // add content to dialog and return
        dialog.setView(content)
        dialog.setOnKeyListener(DialogInterface.OnKeyListener { d, keyCode, event ->
            val button = mapping[keyCode]
            if (button != null) {
                if (event.action == KeyEvent.ACTION_UP) {
                    button.performClick()
                }
                return@OnKeyListener true // also for ignored actions
            }
            false
        })
        dialog.show()
    }

    private val rehideSystemUi = Runnable {
        if (Build.VERSION.SDK_INT >= 19) {
            val flags = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.INVISIBLE
            this@SDLActivity.window.decorView.systemUiVisibility = flags
        }
    }

    override fun onSystemUiVisibilityChange(visibility: Int) {
        if (mFullscreenModeActive && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0 || visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0)) {
            val handler = window.decorView.handler
            if (handler != null) {
                handler.removeCallbacks(rehideSystemUi) // Prevent a hide loop.
                handler.postDelayed(rehideSystemUi, 2000)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val result = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
        nativePermissionResult(requestCode, result)
    }

    companion object {
        private const val TAG = "SDL"
        private const val SDL_MAJOR_VERSION = 2
        private const val SDL_MINOR_VERSION = 26
        private const val SDL_MICRO_VERSION = 5

        /*
    // Display InputType.SOURCE/CLASS of events and devices
    //
    // SDLActivity.debugSource(device.getSources(), "device[" + device.getName() + "]");
    // SDLActivity.debugSource(event.getSource(), "event");
    public static void debugSource(int sources, String prefix) {
        int s = sources;
        int s_copy = sources;
        String cls = "";
        String src = "";
        int tst = 0;
        int FLAG_TAINTED = 0x80000000;

        if ((s & InputDevice.SOURCE_CLASS_BUTTON) != 0)     cls += " BUTTON";
        if ((s & InputDevice.SOURCE_CLASS_JOYSTICK) != 0)   cls += " JOYSTICK";
        if ((s & InputDevice.SOURCE_CLASS_POINTER) != 0)    cls += " POINTER";
        if ((s & InputDevice.SOURCE_CLASS_POSITION) != 0)   cls += " POSITION";
        if ((s & InputDevice.SOURCE_CLASS_TRACKBALL) != 0)  cls += " TRACKBALL";


        int s2 = s_copy & ~InputDevice.SOURCE_ANY; // keep class bits
        s2 &= ~(  InputDevice.SOURCE_CLASS_BUTTON
                | InputDevice.SOURCE_CLASS_JOYSTICK
                | InputDevice.SOURCE_CLASS_POINTER
                | InputDevice.SOURCE_CLASS_POSITION
                | InputDevice.SOURCE_CLASS_TRACKBALL);

        if (s2 != 0) cls += "Some_Unkown";

        s2 = s_copy & InputDevice.SOURCE_ANY; // keep source only, no class;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tst = InputDevice.SOURCE_BLUETOOTH_STYLUS;
            if ((s & tst) == tst) src += " BLUETOOTH_STYLUS";
            s2 &= ~tst;
        }

        tst = InputDevice.SOURCE_DPAD;
        if ((s & tst) == tst) src += " DPAD";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_GAMEPAD;
        if ((s & tst) == tst) src += " GAMEPAD";
        s2 &= ~tst;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tst = InputDevice.SOURCE_HDMI;
            if ((s & tst) == tst) src += " HDMI";
            s2 &= ~tst;
        }

        tst = InputDevice.SOURCE_JOYSTICK;
        if ((s & tst) == tst) src += " JOYSTICK";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_KEYBOARD;
        if ((s & tst) == tst) src += " KEYBOARD";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_MOUSE;
        if ((s & tst) == tst) src += " MOUSE";
        s2 &= ~tst;

        if (Build.VERSION.SDK_INT >= 26) {
            tst = InputDevice.SOURCE_MOUSE_RELATIVE;
            if ((s & tst) == tst) src += " MOUSE_RELATIVE";
            s2 &= ~tst;

            tst = InputDevice.SOURCE_ROTARY_ENCODER;
            if ((s & tst) == tst) src += " ROTARY_ENCODER";
            s2 &= ~tst;
        }
        tst = InputDevice.SOURCE_STYLUS;
        if ((s & tst) == tst) src += " STYLUS";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_TOUCHPAD;
        if ((s & tst) == tst) src += " TOUCHPAD";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_TOUCHSCREEN;
        if ((s & tst) == tst) src += " TOUCHSCREEN";
        s2 &= ~tst;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            tst = InputDevice.SOURCE_TOUCH_NAVIGATION;
            if ((s & tst) == tst) src += " TOUCH_NAVIGATION";
            s2 &= ~tst;
        }

        tst = InputDevice.SOURCE_TRACKBALL;
        if ((s & tst) == tst) src += " TRACKBALL";
        s2 &= ~tst;

        tst = InputDevice.SOURCE_ANY;
        if ((s & tst) == tst) src += " ANY";
        s2 &= ~tst;

        if (s == FLAG_TAINTED) src += " FLAG_TAINTED";
        s2 &= ~FLAG_TAINTED;

        if (s2 != 0) src += " Some_Unkown";

        Log.v(TAG, prefix + "int=" + s_copy + " CLASS={" + cls + " } source(s):" + src);
    }
*/
        var mIsResumedCalled = false
        var mHasFocus = false
        val mHasMultiWindow = Build.VERSION.SDK_INT >= 24

        // Cursor types
        // private static final int SDL_SYSTEM_CURSOR_NONE = -1;
        private const val SDL_SYSTEM_CURSOR_ARROW = 0
        private const val SDL_SYSTEM_CURSOR_IBEAM = 1
        private const val SDL_SYSTEM_CURSOR_WAIT = 2
        private const val SDL_SYSTEM_CURSOR_CROSSHAIR = 3
        private const val SDL_SYSTEM_CURSOR_WAITARROW = 4
        private const val SDL_SYSTEM_CURSOR_SIZENWSE = 5
        private const val SDL_SYSTEM_CURSOR_SIZENESW = 6
        private const val SDL_SYSTEM_CURSOR_SIZEWE = 7
        private const val SDL_SYSTEM_CURSOR_SIZENS = 8
        private const val SDL_SYSTEM_CURSOR_SIZEALL = 9
        private const val SDL_SYSTEM_CURSOR_NO = 10
        private const val SDL_SYSTEM_CURSOR_HAND = 11
        protected const val SDL_ORIENTATION_UNKNOWN = 0
        const val SDL_ORIENTATION_LANDSCAPE = 1
        const val SDL_ORIENTATION_LANDSCAPE_FLIPPED = 2
        const val SDL_ORIENTATION_PORTRAIT = 3
        const val SDL_ORIENTATION_PORTRAIT_FLIPPED = 4

        @JvmField
        var mCurrentOrientation = 0
        protected var mCurrentLocale: Locale? = null

        @JvmField
        var mNextNativeState: NativeState? = null
        var mCurrentNativeState: NativeState? = null

        /** If shared libraries (e.g. SDL or the native application) could not be loaded.  */
        var mBrokenLibraries = true

        // Static overrides for embedding SDL without SDLActivity as the singleton
        var overrideLibraryPath: String? = null
        var overrideArguments: Array<String>? = null

        // Main components
        @JvmField
        var mSingleton: SDLActivity? = null
        var mSurface: SDLSurface? = null
        var mTextEdit: DummyEdit? = null
        protected var mScreenKeyboardShown = false
        var mLayout: ViewGroup? = null
        var mClipboardHandler: SDLClipboardHandler? = null
        var mCursors: Hashtable<Int, PointerIcon>? = null
        var mLastCursorID = 0
        var mMotionListener: SDLGenericMotionListener_API12? = null
        var mHIDDeviceManager: HIDDeviceManager? = null

        // This is what SDL runs in. It invokes SDL_main(), eventually
        @JvmField
        var mSDLThread: Thread? = null

        @JvmStatic
        val motionListener: SDLGenericMotionListener_API12?
            get() {
                if (mMotionListener == null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        mMotionListener = SDLGenericMotionListener_API26()
                    } else if (Build.VERSION.SDK_INT >= 24) {
                        mMotionListener = SDLGenericMotionListener_API24()
                    } else {
                        mMotionListener = SDLGenericMotionListener_API12()
                    }
                }
                return mMotionListener
            }

        fun initialize() {
            // The static nature of the singleton and Android quirkyness force us to initialize everything here
            // Otherwise, when exiting the app and returning to it, these variables *keep* their pre exit values
            mSingleton = null
            mSurface = null
            mTextEdit = null
            mLayout = null
            mClipboardHandler = null
            mCursors = Hashtable()
            mLastCursorID = 0
            mSDLThread = null
            mIsResumedCalled = false
            mHasFocus = true
            mNextNativeState = NativeState.INIT
            mCurrentNativeState = NativeState.INIT
        }

        val currentOrientation: Int
            get() {
                var result = SDL_ORIENTATION_UNKNOWN
                val activity = context as Activity? ?: return result
                val display = activity.windowManager.defaultDisplay
                when (display.rotation) {
                    Surface.ROTATION_0 -> result = SDL_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> result = SDL_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> result = SDL_ORIENTATION_PORTRAIT_FLIPPED
                    Surface.ROTATION_270 -> result = SDL_ORIENTATION_LANDSCAPE_FLIPPED
                }
                return result
            }

        // Called by JNI from SDL.
        @JvmStatic
        fun manualBackButton() {
            mSingleton!!.pressBackButton()
        }

        /* Transition to next state */
        @JvmStatic
        fun handleNativeState() {
            if (mNextNativeState == mCurrentNativeState) {
                // Already in same state, discard.
                return
            }

            // Try a transition to init state
            if (mNextNativeState == NativeState.INIT) {
                mCurrentNativeState = mNextNativeState
                return
            }

            // Try a transition to paused state
            if (mNextNativeState == NativeState.PAUSED) {
                if (mSDLThread != null) {
                    nativePause()
                }
                if (mSurface != null) {
                    mSurface!!.handlePause()
                }
                mCurrentNativeState = mNextNativeState
                return
            }

            // Try a transition to resumed state
            if (mNextNativeState == NativeState.RESUMED) {
                if (mSurface!!.mIsSurfaceReady && mHasFocus && mIsResumedCalled) {
                    if (mSDLThread == null) {
                        // This is the entry point to the C app.
                        // Start up the C app thread and enable sensor input for the first time
                        // FIXME: Why aren't we enabling sensor input at start?
                        mSDLThread = Thread(SDLMain(), "SDLThread")
                        mSurface!!.enableSensor(Sensor.TYPE_ACCELEROMETER, true)
                        mSDLThread!!.start()

                        // No nativeResume(), don't signal Android_ResumeSem
                    } else {
                        nativeResume()
                    }
                    mSurface!!.handleResume()
                    mCurrentNativeState = mNextNativeState
                }
            }
        }

        // Messages from the SDLMain thread
        const val COMMAND_CHANGE_TITLE = 1
        const val COMMAND_CHANGE_WINDOW_STYLE = 2
        const val COMMAND_TEXTEDIT_HIDE = 3
        const val COMMAND_SET_KEEP_SCREEN_ON = 5
        protected const val COMMAND_USER = 0x8000
        var mFullscreenModeActive = false

        // C functions we call
        @JvmStatic
        external fun nativeGetVersion(): String

        @JvmStatic
        external fun nativeSetupJNI(): Int

        @JvmStatic
        external fun nativeRunMain(library: String?, function: String?, arguments: Any?): Int

        @JvmStatic
        external fun nativeLowMemory()

        @JvmStatic
        external fun nativeSendQuit()

        @JvmStatic
        external fun nativeQuit()

        @JvmStatic
        external fun nativePause()

        @JvmStatic
        external fun nativeResume()

        @JvmStatic
        external fun nativeFocusChanged(hasFocus: Boolean)

        @JvmStatic
        external fun onNativeDropFile(filename: String?)

        @JvmStatic
        external fun nativeSetScreenResolution(
            surfaceWidth: Int,
            surfaceHeight: Int,
            deviceWidth: Int,
            deviceHeight: Int,
            rate: Float
        )

        @JvmStatic
        external fun onNativeResize()

        @JvmStatic
        external fun onNativeKeyDown(keycode: Int)

        @JvmStatic
        external fun onNativeKeyUp(keycode: Int)

        @JvmStatic
        external fun onNativeSoftReturnKey(): Boolean

        @JvmStatic
        external fun onNativeKeyboardFocusLost()

        @JvmStatic
        external fun onNativeMouse(button: Int, action: Int, x: Float, y: Float, relative: Boolean)

        @JvmStatic
        external fun onNativeTouch(
            touchDevId: Int, pointerFingerId: Int,
            action: Int, x: Float,
            y: Float, p: Float
        )

        @JvmStatic
        external fun onNativeAccel(x: Float, y: Float, z: Float)

        @JvmStatic
        external fun onNativeClipboardChanged()

        @JvmStatic
        external fun onNativeSurfaceCreated()

        @JvmStatic
        external fun onNativeSurfaceChanged()

        @JvmStatic
        external fun onNativeSurfaceDestroyed()

        @JvmStatic
        external fun nativeGetHint(name: String?): String?

        @JvmStatic
        external fun nativeGetHintBoolean(name: String?, default_value: Boolean): Boolean

        @JvmStatic
        external fun nativeSetenv(name: String?, value: String?)

        @JvmStatic
        external fun onNativeOrientationChanged(orientation: Int)

        @JvmStatic
        external fun nativeAddTouch(touchId: Int, name: String?)

        @JvmStatic
        external fun nativePermissionResult(requestCode: Int, result: Boolean)

        @JvmStatic
        external fun onNativeLocaleChanged()

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun setActivityTitle(title: String?): Boolean {
            // Called from SDLMain() thread and can't directly affect the view
            return mSingleton!!.sendCommand(COMMAND_CHANGE_TITLE, title)
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun setWindowStyle(fullscreen: Boolean) {
            // Called from SDLMain() thread and can't directly affect the view
            mSingleton!!.sendCommand(COMMAND_CHANGE_WINDOW_STYLE, if (fullscreen) 1 else 0)
        }

        /**
         * This method is called by SDL using JNI.
         * This is a static method for JNI convenience, it calls a non-static method
         * so that is can be overridden
         */
        @JvmStatic
        fun setOrientation(w: Int, h: Int, resizable: Boolean, hint: String) {
            if (mSingleton != null) {
                mSingleton!!.setOrientationBis(w, h, resizable, hint)
            }
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun minimizeWindow() {
            if (mSingleton == null) {
                return
            }
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mSingleton!!.startActivity(startMain)
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun shouldMinimizeOnFocusLoss(): Boolean {
            /*
        if (Build.VERSION.SDK_INT >= 24) {
            if (mSingleton == null) {
                return true;
            }

            if (mSingleton.isInMultiWindowMode()) {
                return false;
            }

            if (mSingleton.isInPictureInPictureMode()) {
                return false;
            }
        }

        return true;
*/
            return false
        }

        @JvmStatic
        val isScreenKeyboardShown: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() {
                if (mTextEdit == null) {
                    return false
                }
                if (!mScreenKeyboardShown) {
                    return false
                }
                val imm =
                    getContext()!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                return imm.isAcceptingText
            }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun supportsRelativeMouse(): Boolean {
            // DeX mode in Samsung Experience 9.0 and earlier doesn't support relative mice properly under
            // Android 7 APIs, and simply returns no data under Android 8 APIs.
            //
            // This is fixed in Samsung Experience 9.5, which corresponds to Android 8.1.0, and
            // thus SDK version 27.  If we are in DeX mode and not API 27 or higher, as a result,
            // we should stick to relative mode.
            //
            return if (Build.VERSION.SDK_INT < 27 && isDeXMode) {
                false
            } else motionListener!!.supportsRelativeMouse()
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun setRelativeMouseEnabled(enabled: Boolean): Boolean {
            return if (enabled && !supportsRelativeMouse()) {
                false
            } else motionListener!!.setRelativeMouseEnabled(enabled)
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun sendMessage(command: Int, param: Int): Boolean {
            return if (mSingleton == null) {
                false
            } else mSingleton!!.sendCommand(command, param)
        }

        @JvmStatic
        val context: Context?
            /**
             * This method is called by SDL using JNI.
             */
            get() = getContext()

        @JvmStatic
        val isAndroidTV: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() {
                val uiModeManager = context!!.getSystemService(UI_MODE_SERVICE) as UiModeManager
                if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                    return true
                }
                if (Build.MANUFACTURER == "MINIX" && Build.MODEL == "NEO-U1") {
                    return true
                }
                return if (Build.MANUFACTURER == "Amlogic" && Build.MODEL == "X96-W") {
                    true
                } else Build.MANUFACTURER == "Amlogic" && Build.MODEL.startsWith("TV")
            }
        val diagonal: Double
            get() {
                val metrics = DisplayMetrics()
                val activity = context as Activity? ?: return 0.0
                activity.windowManager.defaultDisplay.getMetrics(metrics)
                val dWidthInches = metrics.widthPixels / metrics.xdpi.toDouble()
                val dHeightInches = metrics.heightPixels / metrics.ydpi.toDouble()
                return Math.sqrt(dWidthInches * dWidthInches + dHeightInches * dHeightInches)
            }

        @JvmStatic
        val isTablet: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() =// If our diagonal size is seven inches or greater, we consider ourselves a tablet.
                diagonal >= 7.0

        @JvmStatic
        val isChromebook: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() = if (context == null) {
                false
            } else context!!.packageManager.hasSystemFeature("org.chromium.arc.device_management")

        @JvmStatic
        val isDeXMode: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() = if (Build.VERSION.SDK_INT < 24) {
                false
            } else try {
                val config = context!!.resources.configuration
                val configClass: Class<*> = config.javaClass
                (configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                        == configClass.getField("semDesktopModeEnabled").getInt(config))
            } catch (ignored: Exception) {
                false
            }

        @JvmStatic
        val displayDPI: DisplayMetrics
            /**
             * This method is called by SDL using JNI.
             */
            get() = context!!.resources.displayMetrics

        @JvmStatic
        val manifestEnvironmentVariables: Boolean
            /**
             * This method is called by SDL using JNI.
             */
            get() {
                try {
                    if (context == null) {
                        return false
                    }
                    val applicationInfo = context!!.packageManager.getApplicationInfo(
                        context!!.packageName, PackageManager.GET_META_DATA
                    )
                    val bundle = applicationInfo.metaData ?: return false
                    val prefix = "SDL_ENV."
                    val trimLength = prefix.length
                    for (key in bundle.keySet()) {
                        if (key.startsWith(prefix)) {
                            val name = key.substring(trimLength)
                            val value = bundle[key].toString()
                            nativeSetenv(name, value)
                        }
                    }
                    /* environment variables set! */return true
                } catch (e: Exception) {
                    Log.v(TAG, "exception $e")
                }
                return false
            }

        @JvmStatic
        val contentView: View?
            // This method is called by SDLControllerManager's API 26 Generic Motion Handler.
            get() = mLayout

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun showTextInput(x: Int, y: Int, w: Int, h: Int): Boolean {
            // Transfer the task to the main thread as a Runnable
            return mSingleton!!.commandHandler.post(ShowTextInputTask(x, y, w, h))
        }

        fun isTextInputEvent(event: KeyEvent): Boolean {

            // Key pressed with Ctrl should be sent as SDL_KEYDOWN/SDL_KEYUP and not SDL_TEXTINPUT
            return if (event.isCtrlPressed) {
                false
            } else event.isPrintingKey || event.keyCode == KeyEvent.KEYCODE_SPACE
        }

        @JvmStatic
        fun handleKeyEvent(v: View?, keyCode: Int, event: KeyEvent, ic: InputConnection?): Boolean {
            val deviceId = event.deviceId
            var source = event.source
            if (source == InputDevice.SOURCE_UNKNOWN) {
                val device = InputDevice.getDevice(deviceId)
                if (device != null) {
                    source = device.sources
                }
            }

//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            Log.v("SDL", "key down: " + keyCode + ", deviceId = " + deviceId + ", source = " + source);
//        } else if (event.getAction() == KeyEvent.ACTION_UP) {
//            Log.v("SDL", "key up: " + keyCode + ", deviceId = " + deviceId + ", source = " + source);
//        }

            // Dispatch the different events depending on where they come from
            // Some SOURCE_JOYSTICK, SOURCE_DPAD or SOURCE_GAMEPAD are also SOURCE_KEYBOARD
            // So, we try to process them as JOYSTICK/DPAD/GAMEPAD events first, if that fails we try them as KEYBOARD
            //
            // Furthermore, it's possible a game controller has SOURCE_KEYBOARD and
            // SOURCE_JOYSTICK, while its key events arrive from the keyboard source
            // So, retrieve the device itself and check all of its sources
            if (isDeviceSDLJoystick(deviceId)) {
                // Note that we process events with specific key codes here
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (onNativePadDown(deviceId, keyCode) == 0) {
                        return true
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    if (onNativePadUp(deviceId, keyCode) == 0) {
                        return true
                    }
                }
            }
            if (source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (isTextInputEvent(event)) {
                        ic?.commitText(event.unicodeChar.toChar().toString(), 1)
                            ?: nativeCommitText(event.unicodeChar.toChar().toString(), 1)
                    }
                    onNativeKeyDown(keyCode)
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    onNativeKeyUp(keyCode)
                    return true
                }
            }
            if (source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
                // on some devices key events are sent for mouse BUTTON_BACK/FORWARD presses
                // they are ignored here because sending them as mouse input to SDL is messy
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_FORWARD) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP ->                     // mark the event as handled or it will be handled by system
                            // handling KEYCODE_BACK by system will call onBackPressed()
                            return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        val nativeSurface: Surface?
            /**
             * This method is called by SDL using JNI.
             */
            get() = if (mSurface == null) {
                null
            } else mSurface!!.nativeSurface
        // Input
        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun initTouch() {
            val ids = InputDevice.getDeviceIds()
            for (id in ids) {
                val device = InputDevice.getDevice(id)
                /* Allow SOURCE_TOUCHSCREEN and also Virtual InputDevices because they can send TOUCHSCREEN events */if (device != null && (device.sources and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN
                            || device.isVirtual)
                ) {
                    var touchDevId = device.id
                    /*
                 * Prevent id to be -1, since it's used in SDL internal for synthetic events
                 * Appears when using Android emulator, eg:
                 *  adb shell input mouse tap 100 100
                 *  adb shell input touchscreen tap 100 100
                 */if (touchDevId < 0) {
                        touchDevId -= 1
                    }
                    nativeAddTouch(touchDevId, device.name)
                }
            }
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun clipboardHasText(): Boolean {
            return mClipboardHandler!!.clipboardHasText()
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun clipboardGetText(): String? {
            return mClipboardHandler!!.clipboardGetText()
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun clipboardSetText(string: String?) {
            mClipboardHandler!!.clipboardSetText(string)
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun createCustomCursor(
            colors: IntArray?,
            width: Int,
            height: Int,
            hotSpotX: Int,
            hotSpotY: Int
        ): Int {
            val bitmap = Bitmap.createBitmap(colors!!, width, height, Bitmap.Config.ARGB_8888)
            ++mLastCursorID
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    mCursors!![mLastCursorID] =
                        PointerIcon.create(bitmap, hotSpotX.toFloat(), hotSpotY.toFloat())
                } catch (e: Exception) {
                    return 0
                }
            } else {
                return 0
            }
            return mLastCursorID
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun destroyCustomCursor(cursorID: Int) {
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    mCursors!!.remove(cursorID)
                } catch (e: Exception) {
                }
            }
            return
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun setCustomCursor(cursorID: Int): Boolean {
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    mSurface!!.pointerIcon = mCursors!![cursorID]
                } catch (e: Exception) {
                    return false
                }
            } else {
                return false
            }
            return true
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun setSystemCursor(cursorID: Int): Boolean {
            var cursor_type = 0 //PointerIcon.TYPE_NULL;
            when (cursorID) {
                SDL_SYSTEM_CURSOR_ARROW -> cursor_type = 1000 //PointerIcon.TYPE_ARROW;
                SDL_SYSTEM_CURSOR_IBEAM -> cursor_type = 1008 //PointerIcon.TYPE_TEXT;
                SDL_SYSTEM_CURSOR_WAIT -> cursor_type = 1004 //PointerIcon.TYPE_WAIT;
                SDL_SYSTEM_CURSOR_CROSSHAIR -> cursor_type = 1007 //PointerIcon.TYPE_CROSSHAIR;
                SDL_SYSTEM_CURSOR_WAITARROW -> cursor_type = 1004 //PointerIcon.TYPE_WAIT;
                SDL_SYSTEM_CURSOR_SIZENWSE -> cursor_type =
                    1017 //PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW;
                SDL_SYSTEM_CURSOR_SIZENESW -> cursor_type =
                    1016 //PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW;
                SDL_SYSTEM_CURSOR_SIZEWE -> cursor_type =
                    1014 //PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW;
                SDL_SYSTEM_CURSOR_SIZENS -> cursor_type =
                    1015 //PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW;
                SDL_SYSTEM_CURSOR_SIZEALL -> cursor_type = 1020 //PointerIcon.TYPE_GRAB;
                SDL_SYSTEM_CURSOR_NO -> cursor_type = 1012 //PointerIcon.TYPE_NO_DROP;
                SDL_SYSTEM_CURSOR_HAND -> cursor_type = 1002 //PointerIcon.TYPE_HAND;
            }
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    mSurface!!.pointerIcon = PointerIcon.getSystemIcon(getContext()!!, cursor_type)
                } catch (e: Exception) {
                    return false
                }
            }
            return true
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun requestPermission(permission: String, requestCode: Int) {
            if (Build.VERSION.SDK_INT < 23) {
                nativePermissionResult(requestCode, true)
                return
            }
            val activity = context as Activity?
            if (activity!!.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(permission), requestCode)
            } else {
                nativePermissionResult(requestCode, true)
            }
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun openURL(url: String?): Int {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(url))
                var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                flags = if (Build.VERSION.SDK_INT >= 21) {
                    flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                } else {
                    flags or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                }
                i.addFlags(flags)
                mSingleton!!.startActivity(i)
            } catch (ex: Exception) {
                return -1
            }
            return 0
        }

        /**
         * This method is called by SDL using JNI.
         */
        @JvmStatic
        fun showToast(
            message: String,
            duration: Int,
            gravity: Int,
            xOffset: Int,
            yOffset: Int
        ): Int {
            if (null == mSingleton) {
                return -1
            }
            try {
                class OneShotTask(
                    var mMessage: String,
                    var mDuration: Int,
                    var mGravity: Int,
                    var mXOffset: Int,
                    var mYOffset: Int
                ) : Runnable {
                    override fun run() {
                        try {
                            val toast = Toast.makeText(mSingleton, mMessage, mDuration)
                            if (mGravity >= 0) {
                                toast.setGravity(mGravity, mXOffset, mYOffset)
                            }
                            toast.show()
                        } catch (ex: Exception) {
                            Log.e(TAG, ex.message!!)
                        }
                    }
                }
                mSingleton!!.runOnUiThread(
                    OneShotTask(
                        message,
                        duration,
                        gravity,
                        xOffset,
                        yOffset
                    )
                )
            } catch (ex: Exception) {
                return -1
            }
            return 0
        }
    }
}
