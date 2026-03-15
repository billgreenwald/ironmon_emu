# Persistence Layer

**Files:** `tracker/persistence/RunData.kt`, `RunRepository.kt`, `ProfileManager.kt`

## Purpose
Saves run history (attempt count, encounter log, trainer log) per ROM to JSON files on device storage.

---

## Data Models (`RunData.kt`)

```kotlin
data class RunData(
    val romCode: String,
    val startTimestamp: Long,
    val encounterLog: MutableList<EncounterEntry>,
    val trainerLog: MutableList<TrainerEntry>,
    val pokemonNotes: MutableMap<Int, String>,
    val routeLog: MutableList<String>,
    val stats: RunStats
)

data class RunStats(
    val attempts: Int,        // incremented each time lead faints
    val centerVisits: Int,
    val trainerBattles: Int,
    val wildEncounters: Int,
    val steps: Int,
    val playTimeMs: Long
)

data class EncounterEntry(speciesId, speciesName, level, location, isWild, timestamp)
data class TrainerEntry(trainerName, location, won, timestamp)
```

---

## RunRepository (`RunRepository.kt`)
Gson-based JSON file storage.

**Storage location:** `context.filesDir/ironmon_run_<profileId>.json`

```kotlin
object RunRepository {
    fun load(context: Context, profileId: String): RunData
    fun save(context: Context, profileId: String, data: RunData)
    fun delete(context: Context, profileId: String)
    fun romCodeMatches(data: RunData, currentCode: String): Boolean
}
```

**Gson dependency:** `com.google.code.gson:gson:2.10.1` — already in `app/build.gradle.kts`

---

## ProfileManager (`ProfileManager.kt`)
Multi-profile support via SharedPreferences (currently single "default" profile for MVP).

```kotlin
object ProfileManager {
    fun getActiveProfileId(context): String     // default = "default"
    fun setActiveProfileId(context, profileId)
    fun listProfiles(context): List<String>
    fun addProfile(context, profileId)
    fun removeProfile(context, profileId)
}
```

**SharedPreferences key:** `"ironmon_profiles"` (comma-separated profile list + active key)

---

## Flow: Run Attempt Increment
1. TrackerPoller detects lead HP == 0 (game over)
2. Sets `isGameOver = true` in TrackerState
3. Calls `RunRepository.load()` → increments `stats.attempts` → `RunRepository.save()`
4. TrackerPanel shows "GAME OVER" banner + "Run N" badge

## Troubleshooting
- **Run count not persisting:** Check `context.filesDir` write permissions (should always be accessible)
- **Wrong profile loaded:** `ProfileManager.getActiveProfileId()` defaults to "default"
- **Data lost after reinstall:** `filesDir` is per-install; use `getExternalFilesDir()` for backup (not implemented)
- **Gson parse error after schema change:** Delete JSON file or handle missing fields with default values
