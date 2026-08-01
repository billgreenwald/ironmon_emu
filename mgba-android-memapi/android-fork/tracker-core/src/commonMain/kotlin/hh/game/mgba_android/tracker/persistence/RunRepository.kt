package hh.game.mgba_android.tracker.persistence

import hh.game.mgba_android.tracker.platform.FileStore
import hh.game.mgba_android.tracker.platform.TrackerLog
import kotlinx.serialization.encodeToString

/**
 * Loads/saves per-profile run data through a [FileStore]. File naming and the JSON wire format
 * are unchanged from the previous Gson implementation, so existing device saves load as-is.
 */
object RunRepository {

    private const val TAG = "RunRepository"

    private fun fileName(profileId: String): String = "ironmon_run_$profileId.json"

    fun load(store: FileStore, profileId: String): RunData {
        val text = store.read(fileName(profileId)) ?: return RunData()
        return try {
            trackerJson.decodeFromString<RunData>(text)
        } catch (e: Exception) {
            TrackerLog.e(TAG, "load failed: $e")
            RunData()
        }
    }

    fun save(store: FileStore, profileId: String, data: RunData) {
        try {
            store.write(fileName(profileId), trackerJson.encodeToString(data))
        } catch (e: Exception) {
            TrackerLog.e(TAG, "save failed: $e")
        }
    }

    fun delete(store: FileStore, profileId: String) {
        store.delete(fileName(profileId))
    }

    fun romCodeMatches(data: RunData, currentCode: String): Boolean =
        data.romCode.isEmpty() || data.romCode == currentCode
}
