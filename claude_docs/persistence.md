# Persistence Layer

**Files (now in the shared `:tracker-core` module):**
`tracker-core/src/commonMain/.../tracker/persistence/RunData.kt`, `RunRepository.kt`,
`ProfileManager.kt`, `LogRepository.kt`, `TrackerJson.kt`, and
`quickload/FamilyCache.kt`. Android implementations of the storage seams live in
`app/.../tracker/platform/AndroidPlatform.kt`.

## Purpose
Saves run history (attempt count, encounter log, trainer log) per ROM to JSON files on device storage.

## Serialization (kotlinx.serialization)
Uses `kotlinx.serialization`, not Gson. Codec is `trackerJson` in `TrackerJson.kt`
(`ignoreUnknownKeys = true`, `encodeDefaults = true`). **Wire format is preserved from the old
Gson models** — `@SerialName` values equal the old `@SerializedName` values and every field is
defaulted — so existing `ironmon_run_*.json` device saves deserialize unchanged. See
`commonTest/.../SerializationTest.kt` for the round-trip + legacy-JSON guards.

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
JSON storage through an injected `FileStore` seam (no `Context` — keeps it common).

**File name:** `ironmon_run_<profileId>.json`. On Android the `FileStore` is backed by
`context.filesDir` (`FilesDirFileStore` in `AndroidPlatform.kt`).

```kotlin
object RunRepository {
    fun load(store: FileStore, profileId: String): RunData
    fun save(store: FileStore, profileId: String, data: RunData)
    fun delete(store: FileStore, profileId: String)
    fun romCodeMatches(data: RunData, currentCode: String): Boolean
}
```

Call sites obtain the store via `AndroidTrackerPlatform.fileStore(context)`.

---

## ProfileManager (`ProfileManager.kt`)
Multi-profile support through an injected `KeyValueStore` seam (currently unwired — single
"default" profile for MVP).

```kotlin
object ProfileManager {
    fun getActiveProfileId(store: KeyValueStore): String     // default = "default"
    fun setActiveProfileId(store: KeyValueStore, profileId)
    fun listProfiles(store: KeyValueStore): List<String>
    fun addProfile(store: KeyValueStore, profileId)
    fun removeProfile(store: KeyValueStore, profileId)
}
```

**Android KeyValueStore** is backed by SharedPreferences `"ironmon_profiles"`
(`AndroidTrackerPlatform.keyValueStore(context)`; comma-separated profile list + active key).

---

## Flow: Run Attempt Increment
1. TrackerPoller detects lead HP == 0 (game over)
2. Sets `isGameOver = true` in TrackerState
3. Calls `RunRepository.load(env.fileStore, …)` → increments `stats.attempts` → `RunRepository.save(env.fileStore, …)`
4. TrackerPanel shows "GAME OVER" banner + "Run N" badge

## Troubleshooting
- **Run count not persisting:** Check the `FileStore` (Android `filesDir`) write path
- **Wrong profile loaded:** `ProfileManager.getActiveProfileId()` defaults to "default"
- **Data lost after reinstall:** `filesDir` is per-install; use `getExternalFilesDir()` for backup (not implemented)
- **Parse error after schema change:** all fields must stay defaulted; `trackerJson` ignores unknown
  keys and `RunRepository.load` falls back to a fresh `RunData()` on failure
