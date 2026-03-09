package org.libsdl.app

import android.os.Process
import android.util.Log
import org.libsdl.app.SDLActivity.Companion.nativeRunMain

/**
 * Simple runnable to start the SDL application
 */
internal class SDLMain : Runnable {
    override fun run() {
        // Runs SDL_main()
        // Use static overrides when embedding SDL without SDLActivity as the singleton
        val library = SDLActivity.overrideLibraryPath
            ?: SDLActivity.mSingleton!!.mainSharedObject
        val function = SDLActivity.mSingleton?.mainFunction ?: "SDL_main"
        val arguments = SDLActivity.overrideArguments
            ?: SDLActivity.mSingleton?.getArguments() ?: arrayOf()
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
        } catch (e: Exception) {
            Log.v("SDL", "modify thread properties failed $e")
        }
        Log.v("SDL", "Running main function $function from library $library")
        nativeRunMain(library, function, arguments)
        Log.v("SDL", "Finished main function")
        SDLActivity.mSDLThread = null
        if (SDLActivity.mSingleton != null && !SDLActivity.mSingleton!!.isFinishing) {
            // Let's finish the Activity
            SDLActivity.mSingleton!!.finish()
        } // else: embedded mode or Activity already being destroyed
    }
}