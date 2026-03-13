package hh.game.mgba_android.tracker.models

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
        val party: List<PokemonData>,
        val battle: BattleState = BattleState.NONE,
        val currentRoute: RouteInfo? = null,
    ) : TrackerState() {
        val leadPokemon: PokemonData? get() = party.firstOrNull()
    }
}
