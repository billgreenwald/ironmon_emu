package hh.game.mgba_android.tracker.models

import hh.game.mgba_android.tracker.data.BagDetailInfo
import hh.game.mgba_android.tracker.data.GameStats
import hh.game.mgba_android.tracker.data.LearnsetInfo
import hh.game.mgba_android.tracker.data.RouteInfo

sealed class TrackerState {
    /** Emulator core not yet ready or ROM not loaded */
    object Disconnected : TrackerState()

    /** ROM loaded but not a supported Gen III game */
    object NoGameLoaded : TrackerState()

    /** Active game with decoded party data */
    data class Active(
        val game: GameVersion,
        val romVersion: Int = 0,   // 0=v1.0, 1=v1.1, 2=v1.2
        val romTitle: String = "",  // 12-char title from GBA header at 0x080000A0
        val party: List<PokemonData>,
        val battle: BattleState = BattleState.NONE,
        val currentRoute: RouteInfo? = null,
        val stats: GameStats? = null,
        val bagDetail: BagDetailInfo? = null,
        val isGameOver: Boolean = false,
        val runAttempts: Int = 0,
        val playerLearnset: LearnsetInfo? = null,
        val enemyLearnset: LearnsetInfo? = null,
        // Route encounter tracking: mapLayoutId → list of seen species IDs (in encounter order)
        val routeEncounters: Map<Int, List<Int>> = emptyMap(),
        val routeVisitOrder: List<Int> = emptyList(),
        // Trainer defeat counts: mapLayoutId → (defeated, total) — read live from SaveBlock1
        val trainerCounts: Map<Int, Pair<Int, Int>> = emptyMap(),
        // Routes the player has physically entered this run
        val visitedRoutes: Set<Int> = emptySet(),
        // Ball picker: shown at run start in the starter lab before the player has any Pokémon
        val showBallPicker: Boolean = false,
        val chosenBall: Int = 0,   // 1=Left, 2=Middle, 3=Right; 0=not yet chosen
    ) : TrackerState() {
        val leadPokemon: PokemonData? get() = party.firstOrNull()
    }
}
