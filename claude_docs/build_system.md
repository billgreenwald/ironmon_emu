# Build System & Environment

## Build Command
```bash
cd /home/bill/ironmon_emu/mgba-android-memapi/android-fork
ANDROID_HOME=/home/bill/Android/Sdk /home/bill/gradle-8.4/bin/gradle :app:assembleDebug --no-daemon
```

## Install + Run
```bash
/home/bill/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/home/bill/Android/Sdk/platform-tools/adb shell am start -n hh.game.mgba_android/.activity.GameActivity
```

## Environment
| Component | Path / Version |
|-----------|---------------|
| Android SDK | `/home/bill/Android/Sdk` |
| Build tools | `34.0.0` |
| Platform | `android-34` |
| NDK | `25.2.9519653` (pinned in `app/build.gradle.kts`) |
| Gradle | `/home/bill/gradle-8.4/bin/gradle` (8.4) |
| AGP | `8.3.1` |
| Kotlin | `1.9.22` |
| Compose BOM | `2023.03.00` |
| Material3 | `1.1.0` |
| Gson | `2.10.1` |
| adb | `/home/bill/Android/Sdk/platform-tools/adb` |

## Key Dependency Notes
- **Material3 1.1.0 compat:** Use `LinearProgressIndicator(progress = Float, ...)` — NOT the lambda `progress = { Float }` form (requires newer BOM)
- **Gson:** Already in deps — no new deps needed for persistence
- **Landscapist Glide:** For Pokemon sprite loading in TrackerPanel

## Project Structure
```
android-fork/
  app/
    src/main/
      java/hh/game/mgba_android/
        activity/       — GameActivity + other activities
        tracker/        — ALL tracker code (see other docs)
      cpp/              — runGame.cpp, memapi_server.h, ards.h/cpp
      res/layout/       — activity_game.xml, padboard.xml
    build.gradle.kts    — deps, ndkVersion, compileSdk
  build.gradle.kts      — AGP version
  settings.gradle.kts   — module config
```

## C++ / JNI Build
- NDK version pinned to `25.2.9519653`
- CMakeLists.txt in `app/src/main/cpp/` (part of mGBA build)
- JNI functions in `runGame.cpp` must match package: `Java_hh_game_mgba_1android_activity_GameActivity_<functionName>`

## Troubleshooting Build Issues
- **CMake error:** Check NDK version matches pinned value in build.gradle.kts
- **Kotlin compile error on Compose:** Check Material3 version — 1.1.0 has different API than 1.2+
- **"Unresolved reference" for tracker class:** Check package declaration matches directory
- **Gradle OOM:** Add `--no-daemon` and increase heap in gradle.properties if needed
- **ADB device not found:** Check USB connection + `adb devices`
