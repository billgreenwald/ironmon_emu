# iOS app — scaffold & setup guide

This directory is the SwiftUI iOS app. It consumes the shared `:tracker-core` KMP logic as a
prebuilt `TrackerCore.xcframework` and renders `TrackerState`. Everything here was written on
Linux and compiles on GitHub Actions macOS runners — there is no local Mac in the loop.

## Status: route B (same mGBA core + lean native frontend) — IMPLEMENTED, awaiting first CI compile

The emulator is built from the **same libmgba** the Android app uses (not a fork of another app),
with a lean Metal/AVAudio/touch frontend. Written on Linux; its first real compile happens on the
macOS CI runner (expect a fix-up round or two — that's what the `ios-simulator` job is for).

- **Shared logic → iOS framework.** `:tracker-core` (`iosArm64` + `iosSimulatorArm64`) →
  `TrackerCore.xcframework`. iOS seams in `iosMain/.../platform/IosPlatform.kt` (`IosTracker`).
- **Emulator core.** `scripts/build-libmgba.sh` builds `libmgba.xcframework` from the fork's
  `app/src/main/cpp/mgba` (recipe validated on Linux: `-DLIBMGBA_ONLY=ON -DM_CORE_GBA=ON`, static).
  `Sources/Emulator/EmulatorCore.{h,mm}` owns the live `mCore` and drives it single-threaded via
  `runFrame`, exposing video/audio/input/memory to Swift.
- **Frontend** (`Sources/App/`): `GameMetalView` (Metal blit of the 240×160 framebuffer),
  `EmulatorAudio` (AVAudioSourceNode, 48 kHz), `GamepadView` (touch → keys), `EmulatorController`
  (ties core+audio+tracker), `MgbaMemoryProvider` (feeds the tracker live reads), and a
  landscape-split `ContentView` (game+controls left, tracker right).
- **CI** (`.github/workflows/ios.yml`): Linux tests + the iOS-compat gate; a macOS **simulator**
  build; a macOS **unsigned `.ipa`** artifact (for Sideloadly); and a signed `.ipa` job.

**To get the beta:** push → the `ios-unsigned-ipa` job produces `IronmonTracker-unsigned-ipa` →
download → install with **Sideloadly** (your Apple ID) → import a GBA ROM in-app.

The app **builds and runs today**, showing "Disconnected" — because no emulator is wired in yet.
The two remaining pieces are yours: **(A) signing secrets** and **(B) the mGBA core**.

---

## A. What YOU must put where — Apple signing (for the `.ipa` job)

Do this once in a browser + your repo settings. Until it's done, the `ios-simulator` job still
works (no signing); only `ios-device-ipa` needs these.

1. **Apple Developer portal** (developer.apple.com/account):
   - Register your test device's **UDID** (iPhone/iPad you'll sideload to).
   - Create a **development signing certificate**; export it as a **`.p12`** with a password.
   - Create a **development provisioning profile** for your app's bundle id, tied to that cert +
     the registered UDID(s). Note its **exact name**.
2. **GitHub → repo → Settings → Secrets and variables → Actions → New repository secret.**
   Add these:

   | Secret name | Value |
   |---|---|
   | `IOS_CERT_P12_BASE64` | `base64 -i cert.p12` (the exported cert) |
   | `IOS_CERT_PASSWORD` | the password you set when exporting the `.p12` |
   | `IOS_PROVISION_PROFILE_BASE64` | `base64 -i profile.mobileprovision` |
   | `IOS_PROVISION_PROFILE_NAME` | the profile's exact name from the portal |
   | `IOS_TEAM_ID` | your 10-char Apple Team ID |
   | `IOS_BUNDLE_ID` | your app bundle id, e.g. `com.yourname.ironmon.tracker` |

   `base64 -i file` prints the encoded blob; paste it as the secret value.
3. **Match the bundle id**: edit `project.yml` → `bundleIdPrefix` and the target's
   `PRODUCT_BUNDLE_IDENTIFIER` to equal your `IOS_BUNDLE_ID`.
4. Run the **ios-device-ipa** job: repo → Actions → "iOS" → Run workflow. Download the
   `IronmonTracker-ipa` artifact and sideload it (AltStore re-signs with your Apple ID).

---

## B. What YOU must decide + wire — the emulator core

The tracker reads live GBA memory. On Android that's the JNI `getMemoryRange` → `core->rawRead8`
/ `busRead8`. iOS needs the same, which means an **iOS mGBA base that builds the native `libmgba`
core** (NOT a libretro wrapper — those don't expose arbitrary reads).

1. **Pick/fork an open-source iOS mGBA app** that (a) builds native `libmgba`, and (b) builds
   headlessly with `xcodebuild` (no GUI-only steps). *(Ask me to research candidates — this is the
   biggest open decision.)*
2. **Expose the live `mCore *`** from that base to Swift.
3. **Fill in `MgbaMemoryProvider`** in `Sources/App/MemoryProvider.swift` (a commented skeleton is
   there): loop bytes, `rawRead8` for `0x08000000..<0x0E000000`, `busRead8` otherwise, guarded on
   the core being live — mirroring `app/src/main/cpp/runGame.cpp:762-772`.
4. In `iOSApp.swift`, swap `ContentView()` to `ContentView(provider: MgbaMemoryProvider(...))` and
   call `viewModel.start(romPath:)` with the loaded ROM path.

Once the provider returns real bytes, the shared `TrackerPoller` lights up with live party/battle
data — identical decoding to Android, since it's the same `:tracker-core`.

---

## Bundling assets (maxData)

The MaxFR/MaxEM support reads `maxdata/*.json` + `*.lua` (currently in
`app/src/main/assets/maxdata/`). For iOS these must be in the **app bundle** under a `maxdata/`
**folder reference** (blue folder, so the path prefix is preserved). Uncomment the `resources:`
block in `project.yml` and confirm they land in the bundle. Vanilla FR/LG/Emerald tracking works
without them; only ROM-hack detection needs them.

---

## Local commands (reference — these run on CI, not on this Linux box)

```bash
# from mgba-android-memapi/android-fork/
./gradlew :tracker-core:assembleTrackerCoreReleaseXCFramework   # build the shared framework
cd iosApp && xcodegen generate                                  # generate the .xcodeproj
# then xcodebuild (see .github/workflows/ios.yml for exact invocations)
```

The Linux-runnable gate for shared-logic changes stays:
`./gradlew :tracker-core:jvmTest :tracker-core:linuxX64Test :tracker-core:compileKotlinLinuxX64`
