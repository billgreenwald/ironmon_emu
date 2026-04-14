package hh.game.mgba_android.tracker.ds.tables

/**
 * Gen IV natures — identical to Gen III. Index = nature ID (personality % 25).
 * boosted/hindered: 0=Atk, 1=Def, 2=SpAtk, 3=SpDef, 4=Spe, -1=none
 */
data class GenIVNature(
    val name: String,
    val boosted: Int,   // stat index boosted (+10%), -1 = neutral
    val hindered: Int,  // stat index hindered (-10%), -1 = neutral
)

object GenIVNatureTable {
    val NATURES = arrayOf(
        GenIVNature("Hardy",   -1,  -1),  // 0
        GenIVNature("Lonely",   0,   1),  // 1  +Atk -Def
        GenIVNature("Brave",    0,   4),  // 2  +Atk -Spe
        GenIVNature("Adamant",  0,   2),  // 3  +Atk -SpA
        GenIVNature("Naughty",  0,   3),  // 4  +Atk -SpD
        GenIVNature("Bold",     1,   0),  // 5  +Def -Atk
        GenIVNature("Docile",  -1,  -1),  // 6
        GenIVNature("Relaxed",  1,   4),  // 7  +Def -Spe
        GenIVNature("Impish",   1,   2),  // 8  +Def -SpA
        GenIVNature("Lax",      1,   3),  // 9  +Def -SpD
        GenIVNature("Timid",    4,   0),  // 10 +Spe -Atk
        GenIVNature("Hasty",    4,   1),  // 11 +Spe -Def
        GenIVNature("Serious", -1,  -1),  // 12
        GenIVNature("Jolly",    4,   2),  // 13 +Spe -SpA
        GenIVNature("Naive",    4,   3),  // 14 +Spe -SpD
        GenIVNature("Modest",   2,   0),  // 15 +SpA -Atk
        GenIVNature("Mild",     2,   1),  // 16 +SpA -Def
        GenIVNature("Quiet",    2,   4),  // 17 +SpA -Spe
        GenIVNature("Bashful", -1,  -1),  // 18
        GenIVNature("Rash",     2,   3),  // 19 +SpA -SpD
        GenIVNature("Calm",     3,   0),  // 20 +SpD -Atk
        GenIVNature("Gentle",   3,   1),  // 21 +SpD -Def
        GenIVNature("Sassy",    3,   4),  // 22 +SpD -Spe
        GenIVNature("Careful",  3,   2),  // 23 +SpD -SpA
        GenIVNature("Quirky",  -1,  -1),  // 24
    )

    fun get(id: Int): GenIVNature = NATURES.getOrElse(id) { NATURES[0] }
}
