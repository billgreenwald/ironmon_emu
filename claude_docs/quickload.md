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

## UPR Mode (overwriteWithRandomizer)

When a family is in UPR mode, `advanceToNext()` calls `overwriteWithRandomizer()`:
1. Resolve the current ROM's DocumentFile using the SAF tree URI (`folderUri`) — gives a `content://` URI
2. Call `context.grantUriPermission("ly.mens.rndpkmn", fileUri, READ | WRITE)` so UPR can open the file
3. Send the URI to UPR's `OverwriteService` via `Messenger`
4. Wait up to 10 s for UPR to reply with randomized ROM bytes in `SharedMemory`
5. Write the received bytes back to the file via `openOutputStream`
6. Return the same path (same file, new content) for `loadRomJNI`

**URI must be `content://`** — `file://` URIs are rejected by UPR on Android 10+ (scoped storage). If `folderUri` is null (user hasn't picked a ROM folder), falls back to `DocumentFile.fromFile()` which produces a `file://` URI and will fail.

**Diagnosing UPR failures:** run `adb logcat -s Quickload` — every step is logged. Key tags:
- `overwriteWithRandomizer: ... uri=...` — shows which URI was sent
- `Granted UPR URI permission` / `grantUriPermission failed` — permission grant result
- `Sent URI to UPR, waiting for reply…` — confirms message dispatched
- `Received N bytes from UPR` — UPR responded successfully
- `UPR randomize timed out after 10s` — UPR didn't reply (install/version issue?)
- `openOutputStream returned null — ROM not written` — content URI write failed

## Troubleshooting
- **Quickload button not visible:** Check `canQuickload()` — requires ROM filename to have a number
- **Wrong next ROM:** Check `lastPlayedNumber` in FamilyCache; clear cache to rescan
- **JNI crash:** Verify JNI function name matches GameActivity declaration + package name
- **ROM not found after advance:** Check SAF permissions for the ROM storage folder
- **Family not detected:** Filename must match `(prefix)(number).(gba|gb)` — e.g., `firered1.gba` ✓, `fire_red.gba` ✗
- **"cannot save ROM" from UPR:** URI sent was `file://` (folderUri null) or `grantUriPermission` failed — check logcat `Quickload` tag
