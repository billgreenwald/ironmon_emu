package hh.game.mgba_android.tracker.platform

/**
 * Reads bundled read-only assets shipped with the app (the maxData JSON + Lua files). Android →
 * AssetManager, iOS → main bundle resources. Returns null if the asset is missing.
 */
interface AssetReader {
    fun readText(path: String): String?
}
