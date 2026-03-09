package com.ironmon.tracker.memory

import com.ironmon.tracker.data.DataHelper
import com.ironmon.tracker.data.GameSettings
import com.ironmon.tracker.data.PokemonDecoder
import com.ironmon.tracker.data.models.GameVersion
import com.ironmon.tracker.data.models.TrackerState
import com.ironmon.tracker.data.tables.MoveNames
import com.ironmon.tracker.data.tables.SpeciesNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls the mGBA memory API at 250ms intervals and emits TrackerState.
 *
 * Lifecycle:
 *   start(scope) — launch polling coroutine
 *   stop()       — cancel polling coroutine
 */
class MemoryPoller(
    private val client: MemoryClient,
) {
    private val _trackerState = MutableStateFlow<TrackerState>(TrackerState.Disconnected)
    val trackerState: StateFlow<TrackerState> = _trackerState

    private var pollJob: Job? = null

    fun start(scope: CoroutineScope) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                _trackerState.value = poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    // ── Polling logic ────────────────────────────────────────────────────────

    private suspend fun poll(): TrackerState {
        // 1. Detect game from ROM header
        val codeBytes = readBytes(GameSettings.ROM_GAME_CODE_ADDR, 4)
            ?: return TrackerState.Disconnected

        val game = GameSettings.detectGame(codeBytes)
        if (game == GameVersion.UNKNOWN) return TrackerState.NoGameLoaded

        val addresses = DataHelper.addressesFor(game)
            ?: return TrackerState.NoGameLoaded

        // 2. Read party count
        val partyCount = client.readU8(addresses.partyCount)
            ?: return TrackerState.Disconnected
        val count = partyCount.coerceIn(0, 6)

        // 3. Read and decode each party slot
        val party = buildList {
            for (slot in 0 until count) {
                val slotAddress = addresses.partyBase + slot * DataHelper.POKEMON_STRUCT_SIZE
                val raw = readBytes(slotAddress, DataHelper.POKEMON_STRUCT_SIZE) ?: continue
                val pokemon = PokemonDecoder.decode(
                    slot      = slot,
                    raw       = raw,
                    nameTable = { id -> SpeciesNames.get(id) },
                    moveTable = { id -> MoveNames.get(id) },
                )
                if (pokemon != null) add(pokemon)
            }
        }

        return TrackerState.Active(game = game, party = party)
    }

    private suspend fun readBytes(address: Long, length: Int): ByteArray? =
        client.readBytes(address, length)

    companion object {
        private const val POLL_INTERVAL_MS = 250L
    }
}
