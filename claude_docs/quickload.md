# Quickload System

**Files:** `tracker/quickload/RomFamily.kt`, `FamilyCache.kt`, `QuickloadManager.kt`
**JNI:** `runGame.cpp` — `g_pendingRomPath` + `loadRomJNI()`
**Wired in:** `GameActivity.kt` (onCreate quickload callback)

## Purpose
On "New Run" button press: advance to the next numbered ROM in the same family, save it as the new game, reload the emulator without restarting the Activity.

## ROM Family Concept
ROMs with names like `firered01.gba`, `firered02.gba`, ... are a "family" (prefix=`firered`, extension=`gba`).
- Parsed by `RomFamilyUtils.parseFamily()` via regex: `(.*?)(\d+)\.(gba|gb)`
- Families cached in `filesDir/rom_family_cache.json` (Gson) to avoid SAF scans every time

## Key Classes

### RomFamily.kt
```kotlin
data class RomFamily(prefix, number, extension, absolutePath, fileName)
data class RomFamilyGroup(prefix, extension, totalCount, lastPlayedNumber, allMemberPaths)
object RomFamilyUtils {
    fun parseFamily(fileName, absolutePath): RomFamily
}
```

### FamilyCache.kt
```kotlin
object FamilyCache {
    fun save(context, groups: List<RomFamilyGroup>)
    fun load(context): List<RomFamilyGroup>
    fun exists(context): Boolean
    fun clear(context)
}
```
**Cache file:** `filesDir/rom_family_cache.json`

### QuickloadManager.kt
```kotlin
object QuickloadManager {
    var currentFamily: RomFamily?
    fun register(context, absolutePath)   // called in GameActivity.onCreate()
    fun unregister()                      // called in GameActivity.onDestroy()
    fun canQuickload(): Boolean
    fun getNextRomPath(context): String?  // checks cache first, falls back to SAF scan
    fun advanceToNext(context): String?   // update saved number → return next path
}
```

## Quickload Call Chain
```
TrackerPanel "New Run" button pressed
    → onQuickload()
    → TrackerPoller.manualNextRun()       (increments run counter)
    → QuickloadManager.advanceToNext()    (get next ROM path, persist number)
    → loadRomJNI(nextPath)               (JNI: set g_pendingRomPath in runGame.cpp)
    → runGame.cpp loop detects g_pendingRomPath → stops current core → loads new ROM
```

## JNI Side (runGame.cpp)
```c
static char g_pendingRomPath[512];  // set by loadRomJNI()

// JNI function (called from Kotlin):
extern "C" JNIEXPORT void JNICALL Java_..._loadRomJNI(JNIEnv*, jobject, jstring path) {
    // copy path → g_pendingRomPath
    // signal emulation loop to stop
}

// In runGame() loop:
if (g_pendingRomPath[0] != '\0') {
    // unload current core
    // load ROM at g_pendingRomPath
    // clear g_pendingRomPath
}
```

## Troubleshooting
- **Quickload button not visible:** Check `canQuickload()` — requires ROM filename to have a number
- **Wrong next ROM:** Check `lastPlayedNumber` in FamilyCache; clear cache to rescan
- **JNI crash:** Verify JNI function name matches GameActivity declaration + package name
- **ROM not found after advance:** Check SAF permissions for the ROM storage folder
- **Family not detected:** Filename must match `(prefix)(number).(gba|gb)` — e.g., `firered1.gba` ✓, `fire_red.gba` ✗
