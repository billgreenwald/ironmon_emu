package hh.game.mgba_android.tracker.tables

/**
 * Gen III type effectiveness chart.
 *
 * Type IDs as stored in ROM (pokefirered/pokeemerald source, confirmed against game data):
 *  0=Normal,   1=Fighting, 2=Flying,   3=Poison,  4=Ground,
 *  5=Rock,     6=Bug,      7=Ghost,    8=Steel,   9=??? (unused)
 *  10=Fire,    11=Water,   12=Grass,   13=Electric,
 *  14=Psychic, 15=Ice,     16=Dragon,  17=Dark
 *
 * Note: 14=Psychic and 15=Ice (NOT swapped). Verified against live game data.
 */
object TypeChart {

    val TYPE_NAMES = mapOf(
        0  to "Normal",   1  to "Fighting", 2  to "Flying",
        3  to "Poison",   4  to "Ground",   5  to "Rock",
        6  to "Bug",      7  to "Ghost",    8  to "Steel",
        10 to "Fire",     11 to "Water",    12 to "Grass",
        13 to "Electric", 14 to "Psychic",  15 to "Ice",
        16 to "Dragon",   17 to "Dark",
    )

    val ALL_TYPES = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17)

    fun typeName(typeId: Int): String = TYPE_NAMES[typeId] ?: "???"

    /** Effectiveness when [attType] attacks a Pokémon of [defType]. */
    fun effectiveness(attType: Int, defType: Int): Float =
        TABLE[attType]?.get(defType) ?: 1.0f

    /** Combined effectiveness against a dual-type Pokémon. */
    fun effectiveness(attType: Int, defType1: Int, defType2: Int): Float {
        val e1 = effectiveness(attType, defType1)
        val e2 = if (defType2 == defType1) 1.0f else effectiveness(attType, defType2)
        return e1 * e2
    }

    /** Full defense chart: all attacker types → multiplier vs (defType1, defType2). */
    fun defenseChart(defType1: Int, defType2: Int): Map<Int, Float> =
        ALL_TYPES.associate { att -> att to effectiveness(att, defType1, defType2) }

    // TABLE[attackerType][defenderType] = multiplier  (only non-1.0 entries)
    private val TABLE: Map<Int, Map<Int, Float>> = mapOf(
        // Normal
        0 to mapOf(5 to 0.5f, 8 to 0.5f, 7 to 0.0f),
        // Fighting — 2x vs Normal, Rock, Steel, Ice(15), Dark; 0.5x vs Flying, Poison, Bug, Psychic(14)
        1 to mapOf(
            0 to 2.0f, 5 to 2.0f, 8 to 2.0f, 15 to 2.0f, 17 to 2.0f,
            2 to 0.5f, 3 to 0.5f, 6 to 0.5f, 14 to 0.5f,
            7 to 0.0f,
        ),
        // Flying
        2 to mapOf(
            1 to 2.0f, 6 to 2.0f, 12 to 2.0f,
            5 to 0.5f, 8 to 0.5f, 13 to 0.5f,
        ),
        // Poison
        3 to mapOf(
            12 to 2.0f,
            3 to 0.5f, 4 to 0.5f, 5 to 0.5f, 7 to 0.5f,
            8 to 0.0f,
        ),
        // Ground
        4 to mapOf(
            3 to 2.0f, 5 to 2.0f, 8 to 2.0f, 10 to 2.0f, 13 to 2.0f,
            6 to 0.5f, 12 to 0.5f,
            2 to 0.0f,
        ),
        // Rock — 2x vs Flying, Bug, Fire, Ice(15)
        5 to mapOf(
            2 to 2.0f, 6 to 2.0f, 10 to 2.0f, 15 to 2.0f,
            1 to 0.5f, 4 to 0.5f, 8 to 0.5f,
        ),
        // Bug — 2x vs Grass, Psychic(14), Dark
        6 to mapOf(
            12 to 2.0f, 14 to 2.0f, 17 to 2.0f,
            1 to 0.5f, 2 to 0.5f, 7 to 0.5f, 8 to 0.5f, 10 to 0.5f,
        ),
        // Ghost — 2x vs Ghost, Psychic(14)
        7 to mapOf(
            7 to 2.0f, 14 to 2.0f,
            17 to 0.5f, 8 to 0.5f,
            0 to 0.0f,
        ),
        // Steel — 2x vs Ice(15), Rock
        8 to mapOf(
            15 to 2.0f, 5 to 2.0f,
            8 to 0.5f, 10 to 0.5f, 11 to 0.5f, 13 to 0.5f,
        ),
        // Fire — 2x vs Bug, Steel, Grass, Ice(15)
        10 to mapOf(
            6 to 2.0f, 8 to 2.0f, 12 to 2.0f, 15 to 2.0f,
            5 to 0.5f, 10 to 0.5f, 11 to 0.5f, 16 to 0.5f,
        ),
        // Water
        11 to mapOf(
            4 to 2.0f, 5 to 2.0f, 10 to 2.0f,
            11 to 0.5f, 12 to 0.5f, 16 to 0.5f,
        ),
        // Grass
        12 to mapOf(
            4 to 2.0f, 5 to 2.0f, 11 to 2.0f,
            2 to 0.5f, 3 to 0.5f, 6 to 0.5f, 8 to 0.5f,
            10 to 0.5f, 12 to 0.5f, 16 to 0.5f,
        ),
        // Electric
        13 to mapOf(
            2 to 2.0f, 11 to 2.0f,
            12 to 0.5f, 13 to 0.5f, 16 to 0.5f,
            4 to 0.0f,
        ),
        // Psychic (14) — 2x vs Fighting, Poison; 0.5x vs Steel, Psychic; 0x vs Dark
        14 to mapOf(
            1 to 2.0f, 3 to 2.0f,
            8 to 0.5f, 14 to 0.5f,
            17 to 0.0f,
        ),
        // Ice (15) — 2x vs Flying, Ground, Grass, Dragon; 0.5x vs Steel, Water, Ice
        15 to mapOf(
            2 to 2.0f, 4 to 2.0f, 12 to 2.0f, 16 to 2.0f,
            8 to 0.5f, 11 to 0.5f, 15 to 0.5f,
        ),
        // Dragon
        16 to mapOf(
            16 to 2.0f,
            8 to 0.5f,
        ),
        // Dark — 2x vs Ghost, Psychic(14)
        17 to mapOf(
            7 to 2.0f, 14 to 2.0f,
            1 to 0.5f, 17 to 0.5f,
        ),
    )
}
