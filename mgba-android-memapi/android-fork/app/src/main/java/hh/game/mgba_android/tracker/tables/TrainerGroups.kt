package hh.game.mgba_android.tracker.tables

import hh.game.mgba_android.tracker.models.GameVersion

/**
 * Trainer classification (Gym/Rival/Elite4/Boss) and the gym-leader → TM reward
 * mapping, keyed by internal trainer ID.
 *
 * Ported from the Lua `TrainerData.lua` (`setupTrainersAsFRLG` classToTrainers
 * groups + `GymTMs`). FRLG only for now — RSE/Emerald trainer-group tables are
 * not yet ported, so on those games the trainer filters/gym-TM view are inert
 * (the "All" trainer list still works).
 */
object TrainerGroups {

    enum class Group { GYM, RIVAL, ELITE4, BOSS }

    /** One gym's badge order, leader, their in-game trainer ID, and the TM they award. */
    data class GymTM(val badge: Int, val leader: String, val leaderTrainerId: Int, val tmNumber: Int)

    // ── FireRed / LeafGreen ────────────────────────────────────────────────
    private val FRLG_GYM = setOf(414, 415, 416, 417, 418, 419, 420, 350)
    private val FRLG_RIVAL = setOf(
        326, 327, 328, 329, 330, 331, 332, 333, 334,
        426, 427, 428, 429, 430, 431, 432, 433, 434,
        435, 436, 437,
    )
    private val FRLG_ELITE4 = setOf(410, 411, 412, 413, 438, 439, 440, 735, 736, 737, 738, 739, 740, 741)
    private val FRLG_BOSS = setOf(317, 348, 349) // Dojo Leader, Giovanni 1 & 2

    private val FRLG_GROUPS: Map<Int, Group> = buildMap {
        FRLG_GYM.forEach { put(it, Group.GYM) }
        FRLG_RIVAL.forEach { put(it, Group.RIVAL) }
        FRLG_ELITE4.forEach { put(it, Group.ELITE4) }
        FRLG_BOSS.forEach { put(it, Group.BOSS) }
    }

    // GymTMs in badge order (leader, trainer ID, TM number). From TrainerData.setupTrainersAsFRLG.
    private val FRLG_GYM_TMS = listOf(
        GymTM(1, "Brock", 414, 39),
        GymTM(2, "Misty", 415, 3),
        GymTM(3, "Lt. Surge", 416, 34),
        GymTM(4, "Erika", 417, 19),
        GymTM(5, "Koga", 418, 6),
        GymTM(6, "Sabrina", 420, 4),
        GymTM(7, "Blaine", 419, 38),
        GymTM(8, "Giovanni", 350, 26),
    )

    private fun isFrlg(game: GameVersion) =
        game == GameVersion.FIRE_RED || game == GameVersion.LEAF_GREEN

    /** Group for a trainer ID, or null if unclassified / non-FRLG game. */
    fun groupOf(game: GameVersion, trainerId: Int): Group? =
        if (isFrlg(game)) FRLG_GROUPS[trainerId] else null

    /** Gym-leader → TM rewards in badge order (empty for non-FRLG games). */
    fun gymTMs(game: GameVersion): List<GymTM> =
        if (isFrlg(game)) FRLG_GYM_TMS else emptyList()
}
