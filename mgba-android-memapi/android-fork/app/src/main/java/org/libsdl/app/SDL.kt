package org.libsdl.app

import android.content.Context
import org.libsdl.app.SDLControllerManager.nativeSetupJNI

/**
 * SDL library initialization
 */
object SDL {
    // This function should be called first and sets up the native code
    // so it can call into the Java classes
    @JvmStatic
    fun setupJNI() {
        SDLActivity.nativeSetupJNI()
        SDLAudioManager.nativeSetupJNI()
        nativeSetupJNI()
    }

    // This function should be called each time the activity is started
    @JvmStatic
    fun initialize() {
        contexc = null
        SDLActivity.initialize()
        SDLAudioManager.initialize()
        SDLControllerManager.initialize()
    }

    @JvmStatic
    @Throws(UnsatisfiedLinkError::class, SecurityException::class, NullPointerException::class)
    fun loadLibrary(libraryName: String?) {
        if (libraryName == null) {
            throw NullPointerException("No library name provided.")
        }
        try {
            // Let's see if we have ReLinker available in the project.  This is necessary for 
            // some projects that have huge numbers of local libraries bundled, and thus may 
            // trip a bug in Android's native library loader which ReLinker works around.  (If
            // loadLibrary works properly, ReLinker will simply use the normal Android method
            // internally.)
            //
            // To use ReLinker, just add it as a dependency.  For more information, see 
            // https://github.com/KeepSafe/ReLinker for ReLinker's repository.
            //
            val relinkClass = contexc!!.classLoader.loadClass("com.getkeepsafe.relinker.ReLinker")
            val relinkListenerClass =
                contexc!!.classLoader.loadClass("com.getkeepsafe.relinker.ReLinker\$LoadListener")
            val contextClass = contexc!!.classLoader.loadClass("android.content.Context")
            val stringClass = contexc!!.classLoader.loadClass("java.lang.String")

            // Get a 'force' instance of the ReLinker, so we can ensure libraries are reinstalled if 
            // they've changed during updates.
            val forceMethod = relinkClass.getDeclaredMethod("force")
            val relinkInstance = forceMethod.invoke(null)
            val relinkInstanceClass: Class<*> = relinkInstance.javaClass

            // Actually load the library!
            val loadMethod = relinkInstanceClass.getDeclaredMethod(
                "loadLibrary",
                contextClass,
                stringClass,
                stringClass,
                relinkListenerClass
            )
            loadMethod.invoke(relinkInstance, contexc, libraryName, null, null)
        } catch (e: Throwable) {
            // Fall back
            try {
                System.loadLibrary(libraryName)
            } catch (ule: UnsatisfiedLinkError) {
                throw ule
            } catch (se: SecurityException) {
                throw se
            }
        }
    }

    // This function stores the current activity (SDL or not)
    var contexc: Context? = null
    @JvmStatic
    fun getContext() = contexc
    @JvmStatic
    fun setContext(context: Context){
        contexc = context
    }
}
