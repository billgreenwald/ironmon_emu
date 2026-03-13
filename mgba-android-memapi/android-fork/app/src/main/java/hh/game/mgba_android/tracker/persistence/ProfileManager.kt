package hh.game.mgba_android.tracker.persistence

import android.content.Context
import androidx.core.content.edit

object ProfileManager {

    private const val PREFS_NAME   = "ironmon_profiles"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE   = "active_profile"
    private const val DEFAULT_ID   = "default"

    fun getActiveProfileId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE, DEFAULT_ID) ?: DEFAULT_ID
    }

    fun setActiveProfileId(context: Context, profileId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_ACTIVE, profileId) }
    }

    fun listProfiles(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PROFILES, DEFAULT_ID) ?: DEFAULT_ID
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun addProfile(context: Context, profileId: String) {
        val current = listProfiles(context).toMutableList()
        if (profileId !in current) {
            current.add(profileId)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_PROFILES, current.joinToString(",")) }
        }
    }

    fun removeProfile(context: Context, profileId: String) {
        val current = listProfiles(context).toMutableList()
        current.remove(profileId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_PROFILES, current.joinToString(",")) }
        // If removed the active one, switch to default
        if (getActiveProfileId(context) == profileId) {
            setActiveProfileId(context, DEFAULT_ID)
        }
    }
}
