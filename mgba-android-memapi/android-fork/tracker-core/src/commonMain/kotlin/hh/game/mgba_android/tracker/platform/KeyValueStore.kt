package hh.game.mgba_android.tracker.platform

/**
 * Small key/value persistence seam. Android → SharedPreferences, iOS → NSUserDefaults.
 * Used by profile management.
 */
interface KeyValueStore {
    fun getString(key: String, default: String?): String?
    fun putString(key: String, value: String)
}
