package hh.game.mgba_android.tracker.tables

/**
 * Maps mapLayoutId → number of wild encounter slots for that route.
 * Counts match RouteData.lua setupRouteInfoAsFRLG() LAND encounter lists
 * (or SURFING count for water-only routes like Route 19/20).
 * Routes not in this table (towns, interiors with no wild encounters) return 0.
 */
object RouteEncounterSlots {

    // FRLG mapLayoutId → total wild encounter slots
    // Source: Ironmon-Tracker/ironmon_tracker/data/RouteData.lua
    private val FRLG = mapOf(
        // ── Outdoor routes ──────────────────────────────────────────────────
        89  to 2,   // Route 1
        90  to 4,   // Route 2
        91  to 6,   // Route 3
        92  to 4,   // Route 4
        93  to 3,   // Route 5
        94  to 3,   // Route 6
        95  to 4,   // Route 7
        96  to 4,   // Route 8
        97  to 3,   // Route 9
        98  to 3,   // Route 10
        99  to 3,   // Route 11
        100 to 4,   // Route 12
        101 to 6,   // Route 13
        102 to 6,   // Route 14
        103 to 6,   // Route 15
        104 to 4,   // Route 16
        105 to 5,   // Route 17
        106 to 5,   // Route 18
        107 to 1,   // Route 19 (surfing only)
        108 to 1,   // Route 20 (surfing only)
        109 to 1,   // Route 21 North
        110 to 3,   // Route 22
        111 to 6,   // Route 23
        112 to 7,   // Route 24
        113 to 7,   // Route 25
        // ── Forests / caves ─────────────────────────────────────────────────
        114 to 4,   // Mt. Moon 1F
        115 to 1,   // Mt. Moon B1F
        116 to 4,   // Mt. Moon B2F
        117 to 5,   // Viridian Forest
        124 to 2,   // Diglett's Cave
        125 to 8,   // Victory Road 1F
        126 to 9,   // Victory Road 2F
        127 to 8,   // Victory Road 3F
        154 to 5,   // Rock Tunnel 1F
        155 to 5,   // Rock Tunnel B1F
        151 to 8,   // Cerulean Cave 1F
        152 to 8,   // Cerulean Cave 2F
        153 to 8,   // Cerulean Cave B1F
        // ── Seafoam Islands ──────────────────────────────────────────────────
        156 to 3,   // Seafoam Islands 1F
        157 to 5,   // Seafoam Islands B1F
        158 to 5,   // Seafoam Islands B2F
        159 to 6,   // Seafoam Islands B3F
        160 to 5,   // Seafoam Islands B4F
        // ── Pokemon Tower ────────────────────────────────────────────────────
        163 to 3,   // Pokemon Tower 3F
        164 to 3,   // Pokemon Tower 4F
        165 to 3,   // Pokemon Tower 5F
        166 to 3,   // Pokemon Tower 6F
        167 to 3,   // Pokemon Tower 7F
        // ── Cinnabar Mansion ─────────────────────────────────────────────────
        143 to 6,   // Pokemon Mansion 1F
        144 to 6,   // Pokemon Mansion 2F
        145 to 6,   // Pokemon Mansion 3F
        146 to 7,   // Pokemon Mansion B1F
        // ── Safari Zone ──────────────────────────────────────────────────────
        147 to 9,   // Safari Zone Center
        148 to 9,   // Safari Zone East
        149 to 9,   // Safari Zone North
        150 to 9,   // Safari Zone West
        // ── Power Plant ──────────────────────────────────────────────────────
        168 to 5,   // Power Plant
    )

    /**
     * Returns the number of encounter slots for [mapLayoutId] in [isHoenn] game context.
     * Returns 0 if the route has no defined wild encounters (towns, gyms, etc.).
     */
    fun get(mapLayoutId: Int, isHoenn: Boolean): Int {
        // Hoenn encounter counts not yet mapped — show only discovered
        if (isHoenn) return 0
        return FRLG[mapLayoutId] ?: 0
    }
}
