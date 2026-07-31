package hh.game.mgba_android.tracker.tables

import hh.game.mgba_android.tracker.models.GameVersion

/**
 * Combined dungeon / gym areas — multiple map layouts that the Lua tracker treats as one
 * location. Standing on any member map shows the AREA name and the AGGREGATE trainer count
 * across all members (rather than a per-floor slice), matching the Lua's
 * RouteData.combineRouteAreas() + getRouteOrAreaName() + TrainersOnRouteScreen.buildScreen().
 *
 * This is why the Lua never needs its per-floor trainer split to be physically accurate — it
 * only ever displays the combined total. We do the same, which also means multi-map dungeons
 * (Mt. Pyre exterior/summit, Weather Institute floors, etc.) no longer show "Unknown Location"
 * or split/overflowing counts.
 *
 * Emerald mapLayoutIds (offset=0). Only areas that actually contain trainers are listed.
 * Ruby/Sapphire shift by +1 above map 124 and are out of scope, so this is Emerald-only.
 */
object CombinedAreas {

    data class Area(val name: String, val members: Set<Int>)

    // Source: RouteData.lua CombinedAreas names + the `area =` membership of each Info entry.
    private val EMERALD: List<Area> = listOf(
        Area("Oceanic Museum",   setOf(86, 87)),
        Area("Lavaridge Gym",    setOf(69, 70)),
        Area("Sootopolis Gym",   setOf(109, 110)),
        Area("Elite Four (Ever Grande City)", setOf(111, 112, 113, 114, 115)),
        Area("Meteor Falls",     setOf(125, 126, 127, 128, 431)),
        Area("Mt. Pyre",         setOf(137, 138, 139, 140, 141, 142, 302, 303)),
        Area("Aqua Hideout",     setOf(143, 144, 145)),
        Area("Seafloor Cavern",  setOf(146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156)),
        Area("Victory Road",     setOf(163, 285, 286)),
        Area("Abandoned Ship",   setOf(186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196)),
        Area("Trick House",      setOf(247, 248, 249, 250, 251, 252, 253, 254)),
        Area("Weather Institute", setOf(271, 272)),
        Area("Space Center",     setOf(275, 276)),
        Area("S.S. Tidal",       setOf(277, 278, 279)),
        Area("Magma Hideout",    setOf(336, 337, 338, 339, 340, 341, 379, 380)),
    )

    private val EMERALD_BY_MAP: Map<Int, Area> = buildMap {
        for (area in EMERALD) for (mapId in area.members) put(mapId, area)
    }

    /** The combined area a map belongs to, or null if it isn't part of one. */
    fun areaOf(version: GameVersion, mapId: Int): Area? =
        if (version == GameVersion.EMERALD) EMERALD_BY_MAP[mapId] else null

    /**
     * Collapse per-map trainer counts into one aggregate per combined area, assigned to
     * EVERY member map (so any floor of a dungeon shows the same combined total). Maps that
     * aren't part of a combined area pass through unchanged.
     *
     * [tableTotals] is the authoritative per-map trainer total from the curated trainer
     * table (mapId → count). The area TOTAL is summed from this, NOT from [counts] — on the
     * NatDex session path, [counts] can contain fallback `(defeats, defeats)` entries for
     * member sub-maps the game reports but the table doesn't list, which would otherwise
     * double-count the total. DEFEATED is summed across all members (so a win attributed to
     * any sub-map still counts) and capped at the total.
     */
    fun collapseCounts(
        version: GameVersion,
        counts: Map<Int, Pair<Int, Int>>,
        tableTotals: Map<Int, Int>,
    ): Map<Int, Pair<Int, Int>> {
        if (version != GameVersion.EMERALD) return counts
        val result = counts.toMutableMap()
        for (area in EMERALD) {
            var defeated = 0
            var total = 0
            var any = false
            for (mapId in area.members) {
                counts[mapId]?.let { defeated += it.first; any = true }
                tableTotals[mapId]?.let { total += it }
            }
            if (any && total > 0) {
                val capped = minOf(defeated, total)
                for (mapId in area.members) result[mapId] = capped to total
            }
        }
        return result
    }
}
