# GameActivity & Layout

**Files:**
- `activity/GameActivity.kt`
- `res/layout/activity_game.xml`
- `res/layout/padboard.xml`

## Purpose
Host activity for the mGBA SDL surface + tracker panel. Owns the 70/30 split between game and tracker.

---

## Layout: activity_game.xml
```
ConstraintLayout (match_parent)
  ├─ Guideline (vertical, 70%) — id: gameZoneBoundary
  ├─ FPS TextView (top-left, elevation 10dp over SDL surface)
  ├─ tools_btn (bottom-left, constrained to left of gameZoneBoundary)
  ├─ <include> padboard.xml (0dp width/height, constrained to boundary)
  └─ [ComposeView added programmatically at leftMargin=gameWidth]
```

The SDL surface (`mSurface`) is resized to `gameWidth` (70% of screen) in `onCreate()`.

---

## GameActivity.onCreate() — Tracker Integration Steps
1. Resolve `gamepath` + `cheat` from intent extras (must be before `super.onCreate()`)
2. `QuickloadManager.register(applicationContext, gamepath)`
3. `super.onCreate()` — loads SDL libs, creates `mSurface`
4. Resize `mSurface` width to `gameWidth = (screenWidth * 0.7).toInt()`
5. Set frame rate hint to 59.7275 fps (GBA rate, for LTPO display compatibility)
6. `MemoryBridge.reader = { addr, len -> getMemoryRange(addr, len) }`
7. `TrackerPoller.start(applicationContext, lifecycleScope)`
8. Create `ComposeView` with `TrackerPanel`:
   - Collects `TrackerPoller.state.collectAsState()`
   - `onQuickload` wired to: manualNextRun → advanceToNext → loadRomJNI → restart
9. `addContentView(composeView, LayoutParams(30% width, MATCH_PARENT, leftMargin=gameWidth))`
10. Process cheats string + inflate overlay controls

## GameActivity.onDestroy()
```kotlin
TrackerPoller.stop()
MemoryBridge.reader = null
QuickloadManager.unregister()
super.onDestroy()
```
**Order matters:** tracker must stop before reader is cleared.

## AndroidManifest
```xml
android:screenOrientation="sensorLandscape"
```
Tracker only works in landscape (hardcoded 70/30 split).

## JNI Declarations in GameActivity
```kotlin
private external fun getMemoryRange(address: Int, length: Int): ByteArray?
private external fun loadRomJNI(path: String)
```
Implemented in `runGame.cpp`.

## Troubleshooting
- **SDL surface wrong size:** Check `mSurface` resize in onCreate — must happen after super.onCreate()
- **ComposeView not showing:** Check leftMargin + layoutParams width (should be remaining 30%)
- **Tracker state not updating:** Check TrackerPoller.start() called + MemoryBridge.reader set
- **Activity leak:** onDestroy order — must stop TrackerPoller before clearing reader
- **Rotation issues:** screenOrientation=sensorLandscape is forced; portrait mode not supported
