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

## Shared module (`:tracker-core`, KMP)
The tracker logic is a Kotlin Multiplatform library (`android` + `jvm` + `linuxX64` targets).
Useful commands (same env prefix as above):
```bash
# Fast logic tests on the JVM (Linux, no device):
gradle :tracker-core:jvmTest --no-daemon
# iOS-compatibility gate — compiles commonMain through Kotlin/Native ON LINUX (no Mac needed):
gradle :tracker-core:compileKotlinLinuxX64 --no-daemon
gradle :tracker-core:linuxX64Test --no-daemon
```
- The `linuxX64` target is **validation-only** (never shipped); it enforces the same restrictions
  the future iOS targets will. Konan toolchain downloads once to `~/.konan`.
- Requires `kotlin.native.distribution.downloadFromMaven=true` in `gradle.properties` (else the
  Kotlin/Native plugin adds an `ivy` repo that violates settings `FAIL_ON_PROJECT_REPOS`).

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
| adb | `/home/bill/Android/Sdk/platform-tools/adb` |
| Kotlin MPP plugin | `1.9.22` (`:tracker-core`) |
| kotlinx.serialization | `1.6.0` (JSON) |
| kotlinx-datetime | `0.4.1` |
| atomicfu | `0.23.1` (library-only, no gradle plugin) |

## Key Dependency Notes
- **Material3 1.1.0 compat:** Use `LinearProgressIndicator(progress = Float, ...)` — NOT the lambda `progress = { Float }` form (requires newer BOM)
- **Persistence uses kotlinx.serialization** (not Gson) — see [`persistence.md`](persistence.md). Gson may still appear elsewhere in the app.
- **Landscapist Glide:** For Pokemon sprite loading in TrackerPanel
- **Common `@Volatile`** needs `import kotlin.concurrent.Volatile` (module opts into `kotlin.ExperimentalStdlibApi`); the JVM one isn't in commonMain.

## Project Structure
```
android-fork/
  tracker-core/         — KMP module: shared tracker logic (see CLAUDE_ARCHITECTURE.md)
    build.gradle.kts    — MPP targets (android/jvm/linuxX64), serialization/datetime/atomicfu deps
    src/commonMain/     — the shared logic; src/commonTest/ — tests
  app/
    src/main/
      java/hh/game/mgba_android/
        activity/       — GameActivity + other activities
        tracker/        — Android-only tracker code: Compose UI, AndroidPlatform.kt seams,
                          LogFileLocator (SAF), QuickloadManager (service IPC)
      cpp/              — runGame.cpp, memapi_server.h, ards.h/cpp
      res/layout/       — activity_game.xml, padboard.xml
    build.gradle.kts    — deps, ndkVersion, compileSdk; depends on :tracker-core
  build.gradle.kts      — AGP + Kotlin MPP/serialization plugin versions
  settings.gradle.kts   — includes :app and :tracker-core
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
