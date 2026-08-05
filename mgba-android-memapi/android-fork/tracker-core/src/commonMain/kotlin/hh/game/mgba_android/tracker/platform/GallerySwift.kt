package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.tables.ImageAssetMap

/**
 * Swift access to the route gallery image map (String-keyed by route name). Image paths are
 * relative to the bundled `assets/` root (e.g. "maps/hidden_items/pewter_city.png").
 */
object GallerySwift {
    fun hasImages(routeName: String): Boolean {
        val r = ImageAssetMap.MAP[routeName] ?: return false
        return r.routeMaps.isNotEmpty() || r.hiddenItems.isNotEmpty()
    }

    fun routeMaps(routeName: String): List<String> = ImageAssetMap.MAP[routeName]?.routeMaps ?: emptyList()
    fun hiddenItems(routeName: String): List<String> = ImageAssetMap.MAP[routeName]?.hiddenItems ?: emptyList()
}
