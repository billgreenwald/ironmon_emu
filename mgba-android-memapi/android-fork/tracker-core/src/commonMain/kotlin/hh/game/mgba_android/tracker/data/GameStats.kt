package hh.game.mgba_android.tracker.data

/**
 * Pure data holder for decoded game stats. Extracted from StatsReader (which stays Android-side
 * for now due to logging) so shared code — e.g. [hh.game.mgba_android.tracker.models.TrackerState]
 * — can reference it from commonMain.
 */
data class GameStats(
    val steps: Long,
    val totalBattles: Long,
    val pokemonCenterVisits: Long,
)
