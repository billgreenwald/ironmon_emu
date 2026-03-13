package hh.game.mgba_android.tracker.tables

/**
 * Experience growth curves for Gen III.
 * Growth groups: 0=Medium Fast, 1=Erratic, 2=Fluctuating, 3=Medium Slow, 4=Fast, 5=Slow
 */
object ExperienceTable {

    /** XP required to reach [level] for growth group [group]. Level range 1..100. */
    fun xpForLevel(group: Int, level: Int): Long {
        if (level <= 1) return 0L
        val n = level.toLong()
        return when (group) {
            0 -> n * n * n                                                             // Medium Fast
            1 -> erratic(level)
            2 -> fluctuating(level)
            3 -> (6L * n * n * n - 15L * n * n + 100L * n - 140L) / 4L               // Medium Slow (may go negative at low levels)
            4 -> (4L * n * n * n) / 5L                                                // Fast
            5 -> (5L * n * n * n) / 4L                                                // Slow
            else -> n * n * n
        }.coerceAtLeast(0L)
    }

    private fun erratic(level: Int): Long {
        val n = level.toLong()
        return when {
            level < 50  -> (n * n * n * (100L - n)) / 50L
            level < 68  -> (n * n * n * (150L - n)) / 100L
            level < 98  -> (n * n * n * ((1911L - 10L * n) / 3L)) / 500L
            else        -> (n * n * n * (160L - n)) / 100L
        }
    }

    private fun fluctuating(level: Int): Long {
        val n = level.toLong()
        return when {
            level < 15  -> (n * n * n * ((n + 1L) / 3L + 24L)) / 50L
            level < 36  -> (n * n * n * (n + 14L)) / 50L
            else        -> (n * n * n * (n / 2L + 32L)) / 50L
        }
    }

    /** Percent of XP gained toward next level. Returns 0.0..1.0 */
    fun xpProgress(group: Int, level: Int, currentXp: Long): Float {
        if (level >= 100) return 1.0f
        val xpThisLevel = xpForLevel(group, level)
        val xpNextLevel = xpForLevel(group, level + 1)
        val span = xpNextLevel - xpThisLevel
        if (span <= 0) return 1.0f
        return ((currentXp - xpThisLevel).toFloat() / span).coerceIn(0f, 1f)
    }
}
