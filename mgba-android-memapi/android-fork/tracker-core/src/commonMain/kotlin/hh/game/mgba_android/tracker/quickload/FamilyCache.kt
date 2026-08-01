package hh.game.mgba_android.tracker.quickload

import hh.game.mgba_android.tracker.persistence.trackerJson
import hh.game.mgba_android.tracker.platform.FileStore
import kotlinx.serialization.encodeToString

/**
 * Caches the discovered ROM family groups through a [FileStore]. JSON wire format matches the
 * previous Gson output (RomFamilyGroup property names unchanged), so an existing cache loads.
 */
object FamilyCache {

    private const val CACHE_FILE = "rom_family_cache.json"

    fun save(store: FileStore, groups: List<RomFamilyGroup>) {
        try {
            store.write(CACHE_FILE, trackerJson.encodeToString(groups))
        } catch (_: Exception) {
        }
    }

    fun load(store: FileStore): List<RomFamilyGroup> =
        try {
            store.read(CACHE_FILE)?.let { trackerJson.decodeFromString<List<RomFamilyGroup>>(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    fun exists(store: FileStore): Boolean = store.exists(CACHE_FILE)

    fun clear(store: FileStore) {
        store.delete(CACHE_FILE)
    }
}
