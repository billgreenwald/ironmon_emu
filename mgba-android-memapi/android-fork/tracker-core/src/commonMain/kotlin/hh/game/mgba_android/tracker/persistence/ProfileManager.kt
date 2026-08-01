package hh.game.mgba_android.tracker.persistence

import hh.game.mgba_android.tracker.platform.KeyValueStore

/**
 * Multi-profile management over a [KeyValueStore]. Keys and the comma-joined profile-list
 * encoding are unchanged; the Android store must be backed by the "ironmon_profiles"
 * SharedPreferences so existing profiles carry over.
 */
object ProfileManager {

    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE   = "active_profile"
    private const val DEFAULT_ID   = "default"

    fun getActiveProfileId(store: KeyValueStore): String =
        store.getString(KEY_ACTIVE, DEFAULT_ID) ?: DEFAULT_ID

    fun setActiveProfileId(store: KeyValueStore, profileId: String) {
        store.putString(KEY_ACTIVE, profileId)
    }

    fun listProfiles(store: KeyValueStore): List<String> {
        val raw = store.getString(KEY_PROFILES, DEFAULT_ID) ?: DEFAULT_ID
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun addProfile(store: KeyValueStore, profileId: String) {
        val current = listProfiles(store).toMutableList()
        if (profileId !in current) {
            current.add(profileId)
            store.putString(KEY_PROFILES, current.joinToString(","))
        }
    }

    fun removeProfile(store: KeyValueStore, profileId: String) {
        val current = listProfiles(store).toMutableList()
        current.remove(profileId)
        store.putString(KEY_PROFILES, current.joinToString(","))
        // If we removed the active one, switch to default.
        if (getActiveProfileId(store) == profileId) {
            setActiveProfileId(store, DEFAULT_ID)
        }
    }
}
