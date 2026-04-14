package hh.game.mgba_android.tracker.ds.tables

/**
 * Gen IV type effectiveness. Type IDs match Gen III ROM values (same encoding).
 *
 * Type ID mapping:
 *  0=Normal, 1=Fighting, 2=Flying, 3=Poison, 4=Ground, 5=Rock,
 *  6=Bug, 7=Ghost, 8=Steel, 9=???, 10=Fire, 11=Water, 12=Grass,
 *  13=Electric, 14=Psychic, 15=Ice, 16=Dragon, 17=Dark
 *
 * Note: Gen IV introduced Physical/Special split but did NOT change type IDs.
 */
object GenIVTypeChart {

    val TYPE_NAMES = mapOf(
        0 to "Normal", 1 to "Fighting", 2 to "Flying", 3 to "Poison",
        4 to "Ground", 5 to "Rock", 6 to "Bug", 7 to "Ghost",
        8 to "Steel", 10 to "Fire", 11 to "Water", 12 to "Grass",
        13 to "Electric", 14 to "Psychic", 15 to "Ice", 16 to "Dragon", 17 to "Dark",
    )

    /** effectiveness[attacker type ID][defender type ID] = multiplier × 100 (0=immune, 50=half, 100=normal, 200=super) */
    private val CHART: Array<IntArray> = run {
        val c = Array(18) { IntArray(18) { 100 } }
        fun set(atk: Int, def: Int, mult: Int) { c[atk][def] = mult }

        // Normal attacking
        set(0, 5, 50); set(0, 7, 0); set(0, 8, 50)
        // Fighting attacking
        set(1, 0, 200); set(1, 2, 50); set(1, 3, 50); set(1, 5, 200)
        set(1, 6, 50); set(1, 7, 0); set(1, 8, 200); set(1, 14, 50); set(1, 15, 200); set(1, 17, 200)
        // Flying attacking
        set(2, 1, 200); set(2, 5, 50); set(2, 8, 50); set(2, 6, 200); set(2, 12, 200); set(2, 13, 50)
        // Poison attacking
        set(3, 3, 50); set(3, 4, 50); set(3, 5, 50); set(3, 7, 50); set(3, 8, 0); set(3, 12, 200)
        // Ground attacking
        set(4, 2, 0); set(4, 3, 200); set(4, 5, 200); set(4, 6, 50); set(4, 8, 200); set(4, 12, 50); set(4, 13, 200)
        // Rock attacking
        set(5, 1, 50); set(5, 2, 200); set(5, 4, 50); set(5, 6, 200); set(5, 8, 50); set(5, 10, 200); set(5, 15, 200)
        // Bug attacking
        set(6, 1, 50); set(6, 2, 50); set(6, 3, 50); set(6, 4, 50); set(6, 7, 50); set(6, 8, 50)
        set(6, 10, 50); set(6, 12, 200); set(6, 14, 200); set(6, 17, 200)
        // Ghost attacking
        set(7, 0, 0); set(7, 1, 0); set(7, 7, 200); set(7, 14, 200); set(7, 17, 50)
        // Steel attacking
        set(8, 3, 0); set(8, 5, 200); set(8, 8, 50); set(8, 10, 50); set(8, 11, 50)
        set(8, 13, 50); set(8, 15, 200); set(8, 16, 50)
        // Fire attacking
        set(10, 5, 50); set(10, 10, 50); set(10, 11, 50); set(10, 12, 200); set(10, 8, 200); set(10, 6, 200); set(10, 15, 200); set(10, 16, 50)
        // Water attacking
        set(11, 5, 200); set(11, 10, 200); set(11, 11, 50); set(11, 12, 50); set(11, 4, 200)
        // Grass attacking
        set(12, 3, 50); set(12, 2, 50); set(12, 6, 50); set(12, 10, 50); set(12, 8, 50)
        set(12, 4, 200); set(12, 11, 200); set(12, 5, 200); set(12, 12, 50); set(12, 16, 50)
        // Electric attacking
        set(13, 2, 200); set(13, 4, 0); set(13, 11, 200); set(13, 12, 50); set(13, 16, 50); set(13, 13, 50)
        // Psychic attacking
        set(14, 1, 200); set(14, 3, 200); set(14, 7, 0); set(14, 8, 50); set(14, 17, 0); set(14, 14, 50)
        // Ice attacking
        set(15, 11, 50); set(15, 2, 200); set(15, 4, 200); set(15, 12, 200); set(15, 16, 200); set(15, 8, 50); set(15, 10, 50); set(15, 15, 50)
        // Dragon attacking
        set(16, 8, 0); set(16, 16, 200)
        // Dark attacking
        set(17, 1, 50); set(17, 7, 200); set(17, 14, 200); set(17, 17, 50); set(17, 8, 50)
        c
    }

    /** Returns effectiveness multiplier ×100. 0=immune, 50=half, 100=neutral, 200=super. */
    fun effectiveness(atkType: Int, defType: Int): Int {
        if (atkType !in 0..17 || defType !in 0..17) return 100
        return CHART[atkType][defType]
    }

    /** Combined effectiveness against a dual-type defender. Returns ×100 (50×50/100 = 25, etc.) */
    fun combinedEffectiveness(atkType: Int, type1: Int, type2: Int): Float {
        val e1 = effectiveness(atkType, type1) / 100f
        val e2 = if (type2 == type1) 1f else effectiveness(atkType, type2) / 100f
        return e1 * e2
    }

    fun typeName(id: Int): String = TYPE_NAMES[id] ?: "???"
}
