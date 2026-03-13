package hh.game.mgba_android.tracker.tables

/**
 * Gen III nature data. Nature = personality % 25.
 * Stat modifiers: +10% / -10% (neutral natures have no modifier).
 * Stats: 0=Atk, 1=Def, 2=SpA, 3=SpD, 4=Spd
 */
data class NatureInfo(
    val name: String,
    val boostedStat: Int,  // -1 = none
    val reducedStat: Int,  // -1 = none
)

object NatureTable {
    val NATURES = arrayOf(
        NatureInfo("Hardy",   -1, -1),  // 0
        NatureInfo("Lonely",   0,  1),  // 1 Atk+/Def-
        NatureInfo("Brave",    0,  4),  // 2 Atk+/Spd-
        NatureInfo("Adamant",  0,  2),  // 3 Atk+/SpA-
        NatureInfo("Naughty",  0,  3),  // 4 Atk+/SpD-
        NatureInfo("Bold",     1,  0),  // 5 Def+/Atk-
        NatureInfo("Docile",  -1, -1),  // 6
        NatureInfo("Relaxed",  1,  4),  // 7 Def+/Spd-
        NatureInfo("Impish",   1,  2),  // 8 Def+/SpA-
        NatureInfo("Lax",      1,  3),  // 9 Def+/SpD-
        NatureInfo("Timid",    4,  0),  // 10 Spd+/Atk-
        NatureInfo("Hasty",    4,  1),  // 11 Spd+/Def-
        NatureInfo("Serious", -1, -1),  // 12
        NatureInfo("Jolly",    4,  2),  // 13 Spd+/SpA-
        NatureInfo("Naive",    4,  3),  // 14 Spd+/SpD-
        NatureInfo("Modest",   2,  0),  // 15 SpA+/Atk-
        NatureInfo("Mild",     2,  1),  // 16 SpA+/Def-
        NatureInfo("Quiet",    2,  4),  // 17 SpA+/Spd-
        NatureInfo("Bashful", -1, -1),  // 18
        NatureInfo("Rash",     2,  3),  // 19 SpA+/SpD-
        NatureInfo("Calm",     3,  0),  // 20 SpD+/Atk-
        NatureInfo("Gentle",   3,  1),  // 21 SpD+/Def-
        NatureInfo("Sassy",    3,  4),  // 22 SpD+/Spd-
        NatureInfo("Careful",  3,  2),  // 23 SpD+/SpA-
        NatureInfo("Quirky",  -1, -1),  // 24
    )

    fun get(natureId: Int): NatureInfo =
        if (natureId in 0..24) NATURES[natureId] else NatureInfo("???", -1, -1)

    /** Short abbreviation for display: e.g. "+Atk/-Def" or "" for neutral */
    fun modifier(natureId: Int): String {
        val n = get(natureId)
        if (n.boostedStat == -1) return ""
        val statNames = arrayOf("Atk", "Def", "SpA", "SpD", "Spd")
        return "+${statNames[n.boostedStat]}/-${statNames[n.reducedStat]}"
    }
}
