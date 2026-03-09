package com.ironmon.tracker.data.models

/**
 * Full snapshot of what the overlay should display.
 * Emitted by MemoryPoller as a StateFlow.
 */
sealed class TrackerState {
    /** mGBA is not running or not reachable on localhost:7777 */
    object Disconnected : TrackerState()

    /** Connected to mGBA but no supported ROM is loaded */
    object NoGameLoaded : TrackerState()

    /** Active game with decoded party data */
    data class Active(
        val game: GameVersion,
        val party: List<PokemonData>,
    ) : TrackerState() {
        val leadPokemon: PokemonData? get() = party.firstOrNull()
    }
}
