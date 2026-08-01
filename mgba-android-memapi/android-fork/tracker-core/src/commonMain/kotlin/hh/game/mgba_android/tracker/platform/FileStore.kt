package hh.game.mgba_android.tracker.platform

/**
 * Named-document persistence seam for the app's private storage (Android filesDir, iOS
 * Documents dir). Names are simple filenames, not paths. Used by run/family-cache persistence.
 */
interface FileStore {
    fun read(name: String): String?
    fun write(name: String, content: String)
    fun delete(name: String)
    fun exists(name: String): Boolean
}
