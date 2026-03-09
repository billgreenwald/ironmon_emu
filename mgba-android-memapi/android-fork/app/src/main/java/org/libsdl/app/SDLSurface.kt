package org.libsdl.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import org.libsdl.app.SDLActivity.Companion.handleKeyEvent
import org.libsdl.app.SDLActivity.Companion.handleNativeState
import org.libsdl.app.SDLActivity.Companion.motionListener
import org.libsdl.app.SDLActivity.Companion.nativeSetScreenResolution
import org.libsdl.app.SDLActivity.Companion.onNativeAccel
import org.libsdl.app.SDLActivity.Companion.onNativeMouse
import org.libsdl.app.SDLActivity.Companion.onNativeOrientationChanged
import org.libsdl.app.SDLActivity.Companion.onNativeResize
import org.libsdl.app.SDLActivity.Companion.onNativeSurfaceChanged
import org.libsdl.app.SDLActivity.Companion.onNativeSurfaceCreated
import org.libsdl.app.SDLActivity.Companion.onNativeSurfaceDestroyed
import org.libsdl.app.SDLActivity.Companion.onNativeTouch

/**
 * SDLSurface. This is what we draw on, so we need to know when it's created
 * in order to do anything useful.
 *
 * Because of this, that's where we set up the SDL thread
 */
class SDLSurface(context: Context) : SurfaceView(context), SurfaceHolder.Callback,
    View.OnKeyListener, OnTouchListener, SensorEventListener {
    // Sensors
    protected var mSensorManager: SensorManager
    protected var mDisplay: Display

    // Keep track of the surface size to normalize touch events
    protected var mWidth: Float
    protected var mHeight: Float

    // Is SurfaceView ready for rendering
    var mIsSurfaceReady: Boolean

    // Startup
    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        setOnKeyListener(this)
        setOnTouchListener(this)
        mDisplay =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        setOnGenericMotionListener(motionListener)

        // Some arbitrary defaults to avoid a potential division by zero
        mWidth = 1.0f
        mHeight = 1.0f
        mIsSurfaceReady = false
    }

    fun handlePause() {
        enableSensor(Sensor.TYPE_ACCELEROMETER, false)
    }

    fun handleResume() {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        setOnKeyListener(this)
        setOnTouchListener(this)
        enableSensor(Sensor.TYPE_ACCELEROMETER, true)
    }

    val nativeSurface: Surface
        get() = holder.surface

    // Called when we have a valid drawing surface
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.v("SDL", "surfaceCreated()")
        onNativeSurfaceCreated()
    }

    // Called when we lose the surface
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.v("SDL", "surfaceDestroyed()")

        // Transition to pause, if needed
        SDLActivity.mNextNativeState = SDLActivity.NativeState.PAUSED
        handleNativeState()
        mIsSurfaceReady = false
        onNativeSurfaceDestroyed()
    }

    // Called when the surface is resized
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int, width: Int, height: Int
    ) {
        Log.v("SDL", "surfaceChanged()")
        mWidth = width.toFloat()
        mHeight = height.toFloat()
        var nDeviceWidth = width
        var nDeviceHeight = height
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                val realMetrics = DisplayMetrics()
                mDisplay.getRealMetrics(realMetrics)
                nDeviceWidth = realMetrics.widthPixels
                nDeviceHeight = realMetrics.heightPixels
            }
        } catch (ignored: Exception) {
        }
        SDLActivity.context?.let { ctx ->
            synchronized(ctx) {
                (ctx as Object).notifyAll()
            }
        }
        Log.v("SDL", "Window size: " + width + "x" + height)
        Log.v("SDL", "Device size: " + nDeviceWidth + "x" + nDeviceHeight)
        nativeSetScreenResolution(width, height, nDeviceWidth, nDeviceHeight, mDisplay.refreshRate)
        onNativeResize()

        // Prevent a screen distortion glitch,
        // for instance when the device is in Landscape and a Portrait App is resumed.
        var skip = false
        val requestedOrientation = SDLActivity.mSingleton?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
            if (mWidth > mHeight) {
                skip = true
            }
        } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            if (mWidth < mHeight) {
                skip = true
            }
        }

        // Special Patch for Square Resolution: Black Berry Passport
        if (skip) {
            val min = Math.min(mWidth, mHeight).toDouble()
            val max = Math.max(mWidth, mHeight).toDouble()
            if (max / min < 1.20) {
                Log.v("SDL", "Don't skip on such aspect-ratio. Could be a square resolution.")
                skip = false
            }
        }

        // Don't skip in MultiWindow.
        if (skip) {
            if (Build.VERSION.SDK_INT >= 24) {
                if (SDLActivity.mSingleton?.isInMultiWindowMode == true) {
                    Log.v("SDL", "Don't skip in Multi-Window")
                    skip = false
                }
            }
        }
        if (skip) {
            Log.v("SDL", "Skip .. Surface is not ready.")
            mIsSurfaceReady = false
            return
        }

        /* If the surface has been previously destroyed by onNativeSurfaceDestroyed, recreate it here */onNativeSurfaceChanged()

        /* Surface is ready */mIsSurfaceReady = true
        SDLActivity.mNextNativeState = SDLActivity.NativeState.RESUMED
        handleNativeState()
    }

    // Key events
    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return handleKeyEvent(v, keyCode, event, null)
    }

    // Touch events
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        /* Ref: http://developer.android.com/training/gestures/multi.html */
        var touchDevId = event.deviceId
        val pointerCount = event.pointerCount
        val action = event.actionMasked
        var pointerFingerId: Int
        var i = -1
        var x: Float
        var y: Float
        var p: Float

        /*
         * Prevent id to be -1, since it's used in SDL internal for synthetic events
         * Appears when using Android emulator, eg:
         *  adb shell input mouse tap 100 100
         *  adb shell input touchscreen tap 100 100
         */if (touchDevId < 0) {
            touchDevId -= 1
        }

        // 12290 = Samsung DeX mode desktop mouse
        // 12290 = 0x3002 = 0x2002 | 0x1002 = SOURCE_MOUSE | SOURCE_TOUCHSCREEN
        // 0x2   = SOURCE_CLASS_POINTER
        if (event.source == InputDevice.SOURCE_MOUSE || event.source == InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_TOUCHSCREEN) {
            var mouseButton = 1
            try {
                val `object` = event.javaClass.getMethod("getButtonState").invoke(event)
                if (`object` != null) {
                    mouseButton = `object` as Int
                }
            } catch (ignored: Exception) {
            }

            // We need to check if we're in relative mouse mode and get the axis offset rather than the x/y values
            // if we are.  We'll leverage our existing mouse motion listener
            val motionListener = motionListener
            x = motionListener!!.getEventX(event)
            y = motionListener.getEventY(event)
            onNativeMouse(mouseButton, action, x, y, motionListener.inRelativeMode())
        } else {
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    i = 0
                    while (i < pointerCount) {
                        pointerFingerId = event.getPointerId(i)
                        x = event.getX(i) / mWidth
                        y = event.getY(i) / mHeight
                        p = event.getPressure(i)
                        if (p > 1.0f) {
                            // may be larger than 1.0f on some devices
                            // see the documentation of getPressure(i)
                            p = 1.0f
                        }
                        onNativeTouch(touchDevId, pointerFingerId, action, x, y, p)
                        i++
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN -> {
                    // Primary pointer up/down, the index is always zero
                    i = 0
                    // Non primary pointer up/down
                    if (i == -1) {
                        i = event.actionIndex
                    }
                    pointerFingerId = event.getPointerId(i)
                    x = event.getX(i) / mWidth
                    y = event.getY(i) / mHeight
                    p = event.getPressure(i)
                    if (p > 1.0f) {
                        // may be larger than 1.0f on some devices
                        // see the documentation of getPressure(i)
                        p = 1.0f
                    }
                    onNativeTouch(touchDevId, pointerFingerId, action, x, y, p)
                }

                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_POINTER_DOWN -> {
                    if (i == -1) {
                        i = event.actionIndex
                    }
                    pointerFingerId = event.getPointerId(i)
                    x = event.getX(i) / mWidth
                    y = event.getY(i) / mHeight
                    p = event.getPressure(i)
                    if (p > 1.0f) {
                        p = 1.0f
                    }
                    onNativeTouch(touchDevId, pointerFingerId, action, x, y, p)
                }

                MotionEvent.ACTION_CANCEL -> {
                    i = 0
                    while (i < pointerCount) {
                        pointerFingerId = event.getPointerId(i)
                        x = event.getX(i) / mWidth
                        y = event.getY(i) / mHeight
                        p = event.getPressure(i)
                        if (p > 1.0f) {
                            // may be larger than 1.0f on some devices
                            // see the documentation of getPressure(i)
                            p = 1.0f
                        }
                        onNativeTouch(touchDevId, pointerFingerId, MotionEvent.ACTION_UP, x, y, p)
                        i++
                    }
                }

                else -> {}
            }
        }
        return true
    }

    // Sensor events
    fun enableSensor(sensortype: Int, enabled: Boolean) {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if (enabled) {
            mSensorManager.registerListener(
                this,
                mSensorManager.getDefaultSensor(sensortype),
                SensorManager.SENSOR_DELAY_GAME, null
            )
        } else {
            mSensorManager.unregisterListener(
                this,
                mSensorManager.getDefaultSensor(sensortype)
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // TODO
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

            // Since we may have an orientation set, we won't receive onConfigurationChanged events.
            // We thus should check here.
            val newOrientation: Int
            val x: Float
            val y: Float
            when (mDisplay.rotation) {
                Surface.ROTATION_90 -> {
                    x = -event.values[1]
                    y = event.values[0]
                    newOrientation = SDLActivity.SDL_ORIENTATION_LANDSCAPE
                }

                Surface.ROTATION_270 -> {
                    x = event.values[1]
                    y = -event.values[0]
                    newOrientation = SDLActivity.SDL_ORIENTATION_LANDSCAPE_FLIPPED
                }

                Surface.ROTATION_180 -> {
                    x = -event.values[0]
                    y = -event.values[1]
                    newOrientation = SDLActivity.SDL_ORIENTATION_PORTRAIT_FLIPPED
                }

                Surface.ROTATION_0 -> {
                    x = event.values[0]
                    y = event.values[1]
                    newOrientation = SDLActivity.SDL_ORIENTATION_PORTRAIT
                }

                else -> {
                    x = event.values[0]
                    y = event.values[1]
                    newOrientation = SDLActivity.SDL_ORIENTATION_PORTRAIT
                }
            }
            if (newOrientation != SDLActivity.mCurrentOrientation) {
                SDLActivity.mCurrentOrientation = newOrientation
                onNativeOrientationChanged(newOrientation)
            }
            onNativeAccel(
                -x / SensorManager.GRAVITY_EARTH,
                y / SensorManager.GRAVITY_EARTH,
                event.values[2] / SensorManager.GRAVITY_EARTH
            )
        }
    }

    // Captured pointer events for API 26.
    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        var action = event.actionMasked
        val x: Float
        val y: Float
        when (action) {
            MotionEvent.ACTION_SCROLL -> {
                x = event.getAxisValue(MotionEvent.AXIS_HSCROLL, 0)
                y = event.getAxisValue(MotionEvent.AXIS_VSCROLL, 0)
                onNativeMouse(0, action, x, y, false)
                return true
            }

            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                x = event.getX(0)
                y = event.getY(0)
                onNativeMouse(0, action, x, y, true)
                return true
            }

            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {

                // Change our action value to what SDL's code expects.
                action = if (action == MotionEvent.ACTION_BUTTON_PRESS) {
                    MotionEvent.ACTION_DOWN
                } else { /* MotionEvent.ACTION_BUTTON_RELEASE */
                    MotionEvent.ACTION_UP
                }
                x = event.getX(0)
                y = event.getY(0)
                val button = event.buttonState
                onNativeMouse(button, action, x, y, true)
                return true
            }
        }
        return false
    }
}
