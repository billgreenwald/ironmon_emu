package hh.game.mgba_android.tracker.ds

import android.content.Context
import android.util.Log
import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.ds.data.DSDataHelper
import hh.game.mgba_android.tracker.ds.data.DSGameSettings
import hh.game.mgba_android.tracker.ds.data.DSPokemonDecoder
import hh.game.mgba_android.tracker.ds.models.DSGameVersion
import hh.game.mgba_android.tracker.ds.models.DSTrackerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object DSTrackerPoller {

    private const val TAG = "DSTrackerPoller"
    private const val POLL_MS = 250L
    private const val MAX_PARTY = 6

    private val _state = MutableStateFlow<DSTrackerState>(DSTrackerState.Disconnected)
    val state: StateFlow<DSTrackerState> = _state

    private var job: Job? = null

    fun start(context: Context, scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            Log.d(TAG, "Poller started")
            while (isActive) {
                try {
                    _state.value = poll()
                } catch (e: Exception) {
                    Log.w(TAG, "Poll error", e)
                    _state.value = DSTrackerState.Disconnected
                }
                delay(POLL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = DSTrackerState.Disconnected
    }

    private fun poll(): DSTrackerState {
        if (MemoryBridge.reader == null) return DSTrackerState.Disconnected

        val version = DSGameSettings.detectVersion()
        if (version == DSGameVersion.UNKNOWN) return DSTrackerState.NoGameLoaded

        val ptrAddr = DSDataHelper.saveBlock1PtrAddress(version)
            ?: return DSTrackerState.NoGameLoaded

        val sb1Base = MemoryBridge.readU32(ptrAddr.toLong())?.toInt()
            ?: return DSTrackerState.Disconnected

        val partyOffset = DSDataHelper.partyOffset(version)
        val countOffset = DSDataHelper.partyCountOffset(version)

        val partyCount = MemoryBridge.readU8((sb1Base + countOffset).toLong())
            ?: return DSTrackerState.Disconnected
        val count = partyCount.coerceIn(0, MAX_PARTY)
        if (count == 0) return DSTrackerState.Active(version, emptyList())

        val partyStart = sb1Base + partyOffset
        val party = (0 until count).mapNotNull { i ->
            val slotAddr = (partyStart + i * DSDataHelper.PARTY_SLOT_STRIDE).toLong()
            val bytes = MemoryBridge.readBytes(slotAddr, DSDataHelper.PARTY_SLOT_STRIDE)
                ?: return@mapNotNull null
            DSPokemonDecoder.decode(bytes)
        }

        return DSTrackerState.Active(version, party)
    }
}
