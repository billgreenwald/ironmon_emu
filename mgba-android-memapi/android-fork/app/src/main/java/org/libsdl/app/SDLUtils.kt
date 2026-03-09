package org.libsdl.app

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup

/**
 * SDLUtils — compatibility shim + embedded SDL initializer.
 *
 * Provides the SDLUtils.init(context, container).setLibraries(...).setArguments(...)
 * builder pattern for hosting SDL inside a non-SDLActivity.
 *
 * Also re-exports key symbols from SDLActivity's companion object so that
 * GameActivity.kt can use them via import.
 */
object SDLUtils {
    var mFullscreenModeActive: Boolean
        get() = SDLActivity.mFullscreenModeActive
        set(value) { SDLActivity.mFullscreenModeActive = value }

    fun onNativeKeyDown(keycode: Int) = SDLActivity.onNativeKeyDown(keycode)
    fun onNativeKeyUp(keycode: Int)   = SDLActivity.onNativeKeyUp(keycode)

    /**
     * Begin SDL initialization for embedding into [container].
     * Call setLibraries() then setArguments() on the returned Builder to complete startup.
     */
    fun init(context: Context, container: View): Builder = Builder(context, container)

    /**
     * Forward a key event to SDL. Returns true if SDL consumed it.
     * When mSingleton is null (embedded mode), falls back to false.
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean =
        SDLActivity.mSingleton?.dispatchKeyEvent(event) ?: false

    class Builder(private val context: Context, private val container: View) {
        private var mLibraries = arrayOf("SDL2", "main")

        fun setLibraries(vararg libs: String): Builder {
            mLibraries = arrayOf(*libs)
            return this
        }

        fun setArguments(vararg args: String?): Builder {
            // 1. Set SDL context so SDL.getContext() works before native libs load
            SDL.setContext(context)
            SDL.initialize()

            // 2. Load native libraries in order (SDL2 first so setupJNI can resolve symbols)
            for (lib in mLibraries) {
                SDL.loadLibrary(lib)
            }

            // 3. Register JNI callbacks (needs SDL2 already loaded)
            SDL.setupJNI()

            // 4. Store override library path and arguments for SDLMain
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val mainLib = mLibraries.last()
            SDLActivity.overrideLibraryPath = "$nativeLibDir/lib$mainLib.so"
            SDLActivity.overrideArguments = args.filterNotNull().toTypedArray()

            // 5. Pre-set SDL state so handleNativeState() can start the thread
            //    once the surface reports ready (mIsSurfaceReady = true).
            SDLActivity.mHasFocus = true
            SDLActivity.mIsResumedCalled = true
            SDLActivity.mNextNativeState = SDLActivity.NativeState.RESUMED

            // 6. Create SDL surface and insert into the host layout
            val surface = SDLSurface(context)
            SDLActivity.mSurface = surface
            (container as? ViewGroup)?.addView(surface)

            return this
        }
    }
}
