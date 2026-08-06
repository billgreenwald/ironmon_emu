package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.quickload.RomFamilyGroup
import hh.game.mgba_android.tracker.quickload.RomFamilyUtils

/** Swift helpers for ROM families (avoids the Swift-keyword `extension` and re-parses numbers). */
object RomFamilySwift {
    fun ext(group: RomFamilyGroup): String = group.extension
    fun memberPaths(group: RomFamilyGroup): List<String> = group.allMemberPaths
    /** Trailing number of a ROM path (e.g. .../firered12.gba → 12; 0 if none). */
    fun numberOfPath(path: String): Int =
        RomFamilyUtils.parseFamily(path.substringAfterLast('/'), path).number ?: 0
}
