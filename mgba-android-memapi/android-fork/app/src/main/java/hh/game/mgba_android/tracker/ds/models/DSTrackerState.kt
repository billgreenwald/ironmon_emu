package hh.game.mgba_android.tracker.ds.models

sealed class DSTrackerState {
    /** MemoryBridge reader is null — DS core not running. */
    object Disconnected : DSTrackerState()

    /** Core running but no valid game detected (UNKNOWN version). */
    object NoGameLoaded : DSTrackerState()

    /** Game active with party data. */
    data class Active(
        val version: DSGameVersion,
        val party: List<DSPokemonData>,   // 1-6 entries; never empty
    ) : DSTrackerState()
}
