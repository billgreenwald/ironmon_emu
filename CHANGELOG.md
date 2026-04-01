# Changelog

## [2.2.0] - 2026-03-31

### Added
- **GachaMon star rating** — the main tracker tab now displays a ★★★★☆☆ star rating (1–5, or 5+) for the lead Pokémon, computed from the same formula used by the Lua Ironmon Tracker's GachaMon system. The score accounts for ability quality, move ratings (with STAB, accuracy, and recoil bonuses), offensive/defensive/speed base stat thresholds, and nature. The numeric score is shown alongside the stars (e.g. `★★★☆☆ (38)`).
- **Rating Ruleset setting** — a new "Rating Ruleset" dropdown in Emulator Settings lets you choose which ruleset to evaluate against: Standard, Ultimate, Kaizo, Survival, Super Kaizo, or Subpar. Banned and adjusted moves/abilities are scored at 0 or 50% per the selected ruleset. Defaults to Standard. Persisted across sessions.

## [2.1.1] - 2026-03-31

### Added
- **Starter ball randomizer** — when a new run begins and the player is in the starter lab with no Pokémon, the tracker now displays a ball picker showing Left / Middle / Right positions. One is randomly highlighted with a ▼ arrow to tell you which starter to grab. A **Reroll** button lets you re-randomize the choice. The picker dismisses automatically once you pick up your starter. Mirrors the Lua tracker's ball picker feature; trigger locations match Lua `RouteData.Locations.IsInLab` (FRLG: Oak's Lab, RSE/Emerald: Route 101).

## [2.1.0] - 2026-03-31

### Added
- **Variable-power move calculations in battle** — move power is now computed live during battle for HP-based, weight-based, friendship-based, and weather-dependent moves. Calculated values display in gold to distinguish them from static labels. Affected moves:
  - **Flail / Reversal** — shows actual power (200/150/100/80/40/20) based on player's current HP
  - **Eruption / Water Spout** — shows computed power (1–150) based on player's current HP
  - **Low Kick** — shows weight bracket (20/40/60/80/100/120) based on enemy species weight (static table from Lua tracker)
  - **Return / Frustration** — shows computed power when friendship is near max (≥100), matching Lua tracker behavior
  - **Hidden Power** — shows power (30–70) computed from player's IVs
  - **Weather Ball** — shows 50 (clear) or 100 (active weather)
- Outside battle, all moves continue showing static labels (`>HP`, `<HP`, `WT`, `VAR`, `>FR`, `<FR`, etc.) unchanged

## [2.0.9] - 2026-03-30

### Added
- **GBA button keyboard bindings** — the "Button Bindings" dialog now has a "GBA Controls" section with all 10 GBA buttons (A, B, L, R, Start, Select, D-pad Up/Down/Left/Right). Each can be rebound to any keyboard key via live capture; defaults match the standard mGBA keyboard layout (X=A, Z=B, Q=L, U=R, Y=Start, N=Select, arrow keys=D-pad). Gamepad button mapping is unchanged.
- **Always hide on-screen controls** — new toggle in Emulator Settings (below "Always show on-screen controls") that permanently hides the padboard. Intended for keyboard or gamepad-only users who don't need touch controls on screen.
- **Tools Menu key binding** — "Tools Menu" added to the bindable actions list so any hardware key can open the in-game tools popup.

### Fixed
- **Button Bindings dialog Save button was off-screen** — the scrollable binding list now caps its height at 55% of the screen height (computed from `LocalConfiguration`), keeping the title and Save/Cancel buttons always visible on all screen sizes.

### Changed
- Key display names now show readable labels ("X", "Z", "DPAD UP") instead of raw numeric codes ("Key(52)") throughout the Button Bindings dialog.

## [2.0.8] - 2026-03-30

### Fixed
- **Catching tutorial battle no longer triggers game-over** — the Old Man / Wally catching demonstration battle is now detected via `sSpecialFlags` (mirrors Lua `Program.updateCatchingTutorial`): when the flag reads 3 the battle is marked as tutorial and the lead-fainted game-over check is suppressed for that battle.

## [2.0.7] - 2026-03-29

### Added
- **Update check** — on each app open the ROM list checks the GitHub releases API for a newer version; if one is found a green banner appears with a tap-to-open link to the release page. The result is cached in SharedPreferences so the banner persists offline until the app is updated.
- **Version display** — current app version (e.g. `v2.0.7`) is shown below the Export Debug Logs button on the ROM list page.

## [2.0.6] - 2026-03-29

### Fixed
- **Audio thread crash on ROM exit** — the Oboe audio stream (`AAudio_1`) kept running after `mCoreThreadJoin` freed `mCoreThread::impl`, causing a null-pointer dereference (`SIGSEGV` at fault addr `0x160`) in the audio callback. Fixed by calling `mOboeDeinit()` before `mCoreThreadJoin()`. Added a defensive `mThread->impl` null check in `onAudioReady` as a secondary guard.
- **Save state load crash** — `QuickLoadState`/`QuickSaveState` called `mCoreLoadState`/`mCoreSaveState` from the JNI thread while the mGBA core thread was running, racing on the GBA timing system (`mTimingSchedule` SIGSEGV). Fixed by wrapping calls with `PauseGame()`/`ResumeGame()` (mCoreThreadInterrupt) so the core is halted before state is touched.
- **Visual freeze after save state load** — `mCoreLoadState` raced with the Oboe audio callback on the `blip_t` audio buffers. The race corrupted the blip read pointer, causing `mCoreSyncProduceAudio` to block forever after the core resumed (GBA CPU blocked → SwappyGL kept swapping the last framebuffer at 60fps, appearing frozen). Fixed by holding `audioBufferMutex` via `mCoreSyncLockAudio`/`mCoreSyncUnlockAudio` around `mCoreLoadState`, and calling `blip_clear` on both channels after load to reset audio to a clean slate.
- **ResumeGame symmetry in save/load state dialogs** — replaced the double-call pattern with a `resumed` guard flag: `ResumeGame()` fires immediately in `onPositive`/`onNegative`, and `onDismiss` calls it only if neither branch ran (back-button/touch-outside dismissal). Exactly one resume per pause, regardless of how the dialog closes.
- **Added GBA-blocked freeze detector** (`mGBA_Freeze` logcat tag) — logs when the GBA core stops delivering video frames for >1.5 s, distinguishing a true GBA thread block from a visually static game state. Also added `mGBA_Load` trace logs for `PauseGame`/`ResumeGame`/`QuickLoadState` showing `interruptDepth` and load result to aid ongoing diagnosis of the post-load freeze.

## [2.0.5] - 2026-03-29

### Added
- **Hide on-screen collapse button** — new toggle in Settings, visible only when "Collapsible tracker panel" is on. When enabled, the ◀/▶ arrow strip is removed from the tracker edge; collapse/expand is then driven exclusively by the "Tracker Open/Close" key binding. A hint below the toggle reminds you to bind the key. When collapsed without the button, the tracker slides fully off-screen (no stub remains).

## [2.0.4] - 2026-03-29

### Fixed
- **30fps lock on retro/handheld devices (root cause fix)** — The Swappy swap interval was set to 16742707ns (59.7275fps = actual GBA rate), which is 76µs longer than one 60Hz frame (16666667ns). Swappy rounded this up to 2 frames = 30fps on all fixed-60Hz devices (Retroid Pocket, Anbernic, etc.). Changed to 16666667ns (60fps) so Swappy always uses exactly 1 frame. Also added `SwappyGL_setMaxAutoSwapIntervalNS(16666667)` to hard-cap auto-doubling, and added a return-value check on `SwappyGL_init`. LTPO tearing prevention on Pixel 7 Pro is unaffected — that is handled by `Surface.setFrameRate(59.7275)` in `surfaceCreated()`, which is unchanged.

## [2.0.3] - 2026-03-29

### Fixed
- **30fps lock on retro/handheld GBA devices** — `SDL_GL_SetSwapInterval(1)` (vsync on) was left as a fallback in `mSDLGLCommonInit`. When Swappy is active, SDL vsync and Swappy both wait on the display — the double-wait halves the effective frame rate to 30fps on 60Hz displays. Now set to `0` so Swappy is the sole frame-pacing authority.

## [2.0.2] - 2026-03-29

### Fixed
- **UPR quickload "cannot save ROM" error** — `overwriteWithRandomizer()` was building a `file://` DocumentFile URI, which UPR-Android's OverwriteService cannot write to under Android 10+ scoped storage. Now resolves the target file from the SAF tree URI (already granted when the user picked their ROM folder) to get a `content://` URI, then explicitly grants UPR-Android (`ly.mens.rndpkmn`) read+write permission on that URI via `grantUriPermission` before sending it to the service. Also added detailed `Quickload` log tags throughout the UPR path to aid future diagnostics.

## [2.0.1] - 2026-03-29

### Fixed
- **Battle stat boost labels were wrong** — stat stages read from `gBattleMons+0x18` were off-by-one (byte 0 is HP, not Atk) and Spe/SpA were swapped. Now reads all 8 bytes and remaps to the correct `[Atk, Def, SpA, SpD, Spe, Acc, Eva]` display order per the Lua tracker source.

## [2.0.0] - 2026-03-28

### Added
- **ROM Family Mode** — each ROM family can now be set to **Batch** (existing behavior: advance through numbered ROMs sequentially) or **UPR** (a single unmodified ROM that gets re-randomized via UPR-Android on every "Next Run"). Switch modes by long-pressing a family card and choosing Family Mode in the settings dialog.
- **UPR-Android integration** — when a family is in UPR mode (or when a batch is exhausted), the tracker binds to UPR-Android's `OverwriteService` (`ly.mens.rndpkmn`), receives the randomized ROM as shared memory, writes it back to disk, and reloads. Requires UPR-Android installed with a version that exposes `OverwriteService`. Requires Android 8.1+ (API 27) for SharedMemory support.
- **Randomizer status indicator** — the ROM list footer now shows "✓ Randomizer installed" (green) or "✗ Randomizer not installed" (grey) so users can tell at a glance whether UPR mode will work.
- **Single-ROM families now appear in the ROM list** — previously only families with 2+ numbered ROMs were shown; a single un-numbered ROM now gets its own family card, enabling UPR-mode solo play.
- **Mode chips on family cards** — every family card shows a blue **BATCH** or orange **UPR** badge so the current mode is always visible at a glance. UPR cards also show "UPR · re-randomized each run" as a subtitle.
- **Family Settings dialog: mode selector** — the long-press settings dialog includes a Batch / UPR chip toggle above the ROM number field. The ROM number field is hidden when UPR is selected (not applicable). Selecting UPR when UPR-Android is not installed shows a red "not installed" error and disables Save.
- **Family tile updates immediately** — switching mode in the settings dialog now refreshes the family card badge in real time without requiring an app restart or ROM reload.
- **`loadNextRom()` helper in GameActivity** — extracted shared quickload logic (used by both the tracker panel button and the game-over Next Run path) into a single private function.

## [1.3.9] - 2026-03-28

### Fixed
- **Audio continues when backgrounded (Z Flip 4)** — `onPause` now always mutes audio when the app leaves foreground; `onWindowFocusChanged` restores the user's mute preference when focus is regained. Fixes audio playing through the SDL AudioTrack thread while the phone is folded/backgrounded.

## [1.3.8] - 2026-03-28

### Added
- **Controls opacity slider** — Emulator Settings dialog (accessible from ROM list and in-game Tools → Settings) now includes a slider (0–100%) to adjust on-screen controls transparency; default 70%
- **Controls scale slider** — Emulator Settings dialog now includes a slider (50–150%) to resize the on-screen controls; default 100%
- **Close ROM** option in Tools menu — shows a "Return to game list?" confirmation dialog; terminates the process cleanly so the ROM list is fully responsive
- **Crash log** — unhandled exceptions are appended to `crash_log.txt` in the app's external files directory for easier debugging (`adb pull` to retrieve)

### Fixed
- **D-pad drag** — finger tracks across the 4 directional buttons: sliding from one direction to another releases the first and fires the second in real time; sliding off releases with no press
- **Multi-touch action buttons** — A, B, L, R, Start, and Select now each use independent per-finger touch listeners, so holding B while pressing a d-pad direction (or any combination of action buttons) works simultaneously
- **App non-responsive after Close ROM** — `finish()` alone left the SDL/mGBA core thread alive; now calls `killProcess()` after finish, matching the quickload pattern

## [1.3.7] - 2026-03-28

### Fixed
- **Xiaomi/MIUI phantom gamepad** — virtual input devices registered by MIUI were falsely detected as gamepads, hiding on-screen controls on Xiaomi devices at launch. Fixed by skipping virtual devices (`InputDevice.isVirtual`) in gamepad detection.

## [1.3.6] - 2026-03-28

### Added
- **General button binding system** — Settings dialog now has a "Set Keybindings" button that opens a dedicated Key Bindings dialog. All six emulator actions (Fast Forward Hold, Save State, Load State, Tracker Open/Close, Next Run, Mute Toggle) can be bound to any physical controller button via live capture: tap an action row, press a button on the controller, binding is saved. Save/Cancel at the bottom. Bindings persist across sessions.
- **One-time migration** — existing "Trigger Button" preference (L2/R2/X/Y chip) is automatically migrated to the new Fast Forward binding on first launch; no manual re-binding needed.

### Changed
- **Collapsible tracker panel toggle** is now always visible in Settings. In overlay modes (100%/0% or 0%/100%) it is greyed out and locked to ON (overlay modes always require collapse/expand behavior).

### Removed
- Removed the 4-chip "Trigger Button" selector from the Settings dialog (superseded by Key Bindings).

## [1.3.5] - 2026-03-28

### Fixed
- **Crash on ROM load** — `ResumeGame()` was being called from `onWindowFocusChanged` on initial launch before the mGBA core thread was initialized, causing SIGSEGV (null pointer dereference in `mCoreThreadContinue → pthread_mutex_lock`). Fix: `ResumeGame()` is now only scheduled when returning from background (after the core has been started and paused at least once), not on the initial activity start.

## [1.3.4] - 2026-03-28

### Added
- **Tracker Size dropdown** in Tools menu — choose any 10%-increment split from 100%/0% to 0%/100% without restarting the game; change takes effect immediately
- **Overlay mode** for 100%/0% (Game Overlay) and 0%/100% (Tracker Overlay) splits — both panels go full-screen and the tracker toggle arrow slides the panel on/off-screen
- **Settings dialog dropdown** — the 3-chip Game/Tracker split selector is replaced with a full 11-option dropdown; the "Collapsible tracker panel" toggle is hidden in overlay modes (forced on automatically)
- Font scale now adjusts proportionally as tracker width changes (scales up for wider tracker panels)

## [1.3.3] - 2026-03-27

### Changed
- Route encounter detail sheet simplified to match opponent tab info only: removed individual base stats (HP/Atk/Def/SpA/SpD/Spe) and abilities
- Route encounter sheet now shows which route(s) the Pokémon was seen on ("Wild on: Route 1, Route 22")
- Route encounter sheet now shows revealed moves (tappable for move detail) when the sheet is opened mid-battle against that species

## [1.3.2] - 2026-03-27

### Fixed
- Release signing configured — APKs are now signed with a stable release keystore instead of the per-machine debug keystore, fixing "application appears to be invalid" on Samsung Knox / flagship devices when sideloading

## [1.3.1] - 2026-03-27

### Added
- **Turbo mode performance logging** — when fast-forward is active, per-frame swap timing is measured via `clock_gettime(CLOCK_MONOTONIC)` around `SwappyGL_swap()`; any frame taking >33ms logs a `mGBA_Perf` warning with duration and cumulative stall count. FPS divergence (actual < 80% of target) and thermal headroom (API 29+, threshold < 0.5) are also logged every 500ms from the FPS coroutine. Run `adb logcat -s mGBA_Perf` to observe. Stall count is also exposed via `getStallCount()` JNI for future UI display.

## [1.3.0] - 2026-03-27

### Added
- **Route encounter detail sheet** — tapping any discovered wild Pokémon sprite in the Routes tab opens a bottom sheet showing the Pokémon's sprite, name, types, base stats (HP/Atk/Def/SpA/SpD/Spe), BST, evolution info, and both abilities. Tapping the type chip row opens the Type Defense sheet; tapping an ability name opens the Ability detail sheet. Undiscovered `?` slots remain non-tappable.

## [1.2.15] - 2026-03-25

### Fixed
- Route encounters no longer persist into the next run — removing `resetGameOver()` from the GAME OVER banner (the run-count double-increment fix in 1.2.13) had the side effect of stopping route data from being cleared on new runs. `manualNextRun()` now always calls `resetGameOver()` after handling the run count, so encounters and revealed moves are wiped at the start of each new run regardless of how it was triggered.

## [1.2.14] - 2026-03-25

### Changed
- Settings dialog: speed-trigger button options reduced to L2, R2, X, and Y only (removed L and R shoulder buttons)
- Settings dialog: chip rows now wrap to the next line on narrow screens instead of overflowing horizontally (`FlowRow`)
- Settings dialog: "Show FPS" toggle moved to the bottom of the dialog
- Settings dialog: "Always show on-screen controls" toggle no longer shows a subtitle — the label alone is sufficient
- Settings dialog: removed capture-from-gamepad feature (auto-detect / default-trigger buttons) — speed trigger is now set by tapping a labeled chip directly

## [1.2.13] - 2026-03-25

### Fixed
- Run count double-incrementing on death: the GAME OVER banner button was calling `resetGameOver()` (setting `isGameOver=false`) before invoking the quickload, which caused `manualNextRun()` to see a false `isGameOver` and increment a second time. Removed the redundant `resetGameOver()` call — the process is killed by quickload anyway
- Additional guard: `manualNextRun()` now uses `AtomicBoolean.getAndSet` so it only increments when transitioning from non-game-over state, preventing any remaining double-count edge cases

## [1.2.12] - 2026-03-23

### Fixed
- Collapsible tracker arrow (`◀`/`▶`) was pushed off-screen when the panel collapsed — the game surface now stops 24dp short of the full width so the arrow strip stays visible
- Arrow direction swapped: `▶` when expanded (tap to collapse), `◀` when collapsed (tap to expand)
- Removed slide animation on collapse/expand — tracker panel now shows/hides instantly

## [1.2.11] - 2026-03-23

### Changed
- APK output file renamed to `ironmon_emulator.apk`

## [1.2.10] - 2026-03-22

### Added
- **Configurable game/tracker split ratio** — choose 70/30, 80/20, or 90/10 in Emulator Settings; tracker fonts scale automatically to fit the narrower panel
- **Collapsible tracker panel** — enable in settings to get a 24dp arrow strip that hides/shows the tracker with a tap or horizontal swipe, giving the game the full screen when collapsed
- **Always show on-screen controls** toggle in settings — disables auto-hide when a gamepad is detected (workaround for controller misfire on padboard buttons)
- **Export Debug Logs** button at the bottom of the ROM list page — captures `logcat` output to a timestamped file and opens the system share sheet

### Fixed
- 19 Psychic-type moves (Future Sight, Calm Mind, Hypnosis, Agility, Reflect, Light Screen, Rest, Amnesia, Kinesis, Psywave, Meditate, Teleport, Barrier, Role Play, Magic Coat, Skill Swap, Imprison, Cosmic Power, Psycho Boost) were incorrectly showing as Ice type — corrected against Lua tracker `PokemonData.TypeIndexMap` (Psychic=14, Ice=15 in ROM)

## [1.2.9] - 2026-03-17

### Added
- Route header now shows trainer count (`Trainers: X/Y`) next to the route name above the tabs — turns green when all trainers on the current route are defeated

## [1.2.8] - 2026-03-17

### Fixed
- Type defense sheet is now scrollable when fully expanded — prevents content being cut off for Pokémon with many weakness/resistance entries
- Type defense sheet now hides 1× (neutral) matchups — only weaknesses, resistances, and immunities are shown
- Variable-power moves (Magnitude, Endeavor, Low Kick, Flail, Reversal, Return, Frustration, Hidden Power, Eruption, Water Spout, Present, Spit Up, OHKO moves, and others) now display the correct label (e.g. `RNG`, `WT`, `<HP`, `>HP`, `>FR`, `<FR`, `VAR`, `100x`, `—`) instead of `1` in the move table and move detail sheet — matching Lua tracker display conventions

## [1.2.7] - 2026-03-16

### Fixed
- Oak's Lab trainer count now shows `0/1` / `1/1` instead of `0/3` — only one rival fight occurs per run (which of the 3 trainer flag IDs fires depends on starter choice); all 3 flags are still checked so the defeat is detected regardless of which starter was picked

## [1.2.6] - 2026-03-16

### Fixed
- MoveStatsTable: Ice and Psychic type IDs were swapped throughout — 12 Ice-type moves (including Ice Beam, Blizzard, Powder Snow, Icy Wind, Sheer Cold) were coded as Psychic, and 9 Psychic moves (including Psychic, Psybeam, Confusion, Extrasensory) were coded as Ice. All corrected to match Lua tracker type constants (Ice=15, Psychic=14)
- EvolutionLevel: Gen III section was keyed by national Dex numbers instead of internal ROM species IDs, causing wrong evolution levels (e.g. Milotic showed "evo lv45", Swablu showed wrong level). Completely rebuilt using correct internal IDs from PokemonData.lua `idInternalToNat` mapping

## [1.2.5] - 2026-03-16

### Fixed
- Route tab now shows all wild Pokémon slots as `?` immediately when entering a route with defined encounters, instead of waiting until the first battle — matches Lua tracker discovery display
- ItemTable corrected against Lua tracker source: multiple sections had wrong IDs (held items off by 2, sellable items off by 4, contest scarves/key items/TMs/HMs all shifted). Shell Bell now correctly shows as Shell Bell instead of Silk Scarf, etc.

## [1.2.0] - 2026-03-15

### Added
- Route image gallery overlay: tapping any route name with available images opens a full-screen tabbed gallery (Maps + Hidden Items) above the game
- 106 hidden-item location images sourced from the FR/LG imgur album, organized by route/dungeon
- 11 dungeon/building route maps (Mt. Moon, Rock Tunnel, Power Plant, Rocket Hideout, Safari Zone, Seafoam Islands, Silph Co., Victory Road, Poke Mansion, Saffron Gym, S.S. Anne)
- `ImageAssetMap` — explicit route-name → asset-path mapping covering 46 FRLG locations
- `GalleryOverlay` — full-screen Compose Dialog with HorizontalPager, ◀ ▶ navigation, and per-tab empty state
- Routes with images show a `↗` indicator in both the header and the Routes tab
- `download_imgur_items.py` — Imgur API script for fetching additional hidden-item images (gitignored)

## [1.1.1] - 2026-03-15

### Fixed
- Gen III Pokémon with internal species ID > 386 (e.g. Beldum = 398, Metang = 399, Metagross = 400, and others) were silently rejected by an incorrect `in 1..386` bounds check. The Lua tracker uses 411 as `baseGameTotal` — all three checks updated to `in 1..411`
- Affected: enemy battle detection, wild encounter recording, and learnset lookup (`TrackerPoller`, `LearnsetReader`)

## [1.1.0] - 2026-03-15

### Added
- Trainer defeat counts in the Routes tab: every location with trainers (gyms, dungeons, routes) now shows `(defeated/total)` next to its name, read live from SaveBlock1 trainer flags — matching the `TrainersOnRouteScreen.lua` display in the reference tracker
- Trainer-only locations (gyms with no wild Pokémon) now appear in the Routes tab with a "no wild Pokémon" note instead of being hidden
- Trainer tables ported from `RouteData.lua` for all 5 supported games (FR/LG, Emerald, Ruby, Sapphire)
- `gameFlagsOffset` added to `GameAddresses` for all game variants (FR/LG: 0xEE0, Emerald: 0x1270, Ruby/Sapphire: 0x1220 — from Lua tracker JSON files)

## [1.0.6] - 2026-03-15

### Fixed
- Opponent tab blank when facing a trainer's non-first Pokémon (e.g. Beldum as last mon): level, HP, and PP were always read from enemy party slot 0 instead of the active slot. Now uses `gBattlerPartyIndexes[2]` (matching Lua tracker) to find the correct slot.

## [1.0.5] - 2026-03-15

### Changed
- Move table: stacked Unicode arrows (⇈/↑/↓/⇊) replace side-by-side text (▲▲/▲/▼/▼▼) for type effectiveness
- Move table: accuracy column added; Pwr/Eff/Acc/PP columns center-aligned; move name font reduced to 13sp

## [1.0.4] - 2026-03-15

### Fixed
- Route encounters now persist across app restarts — closing and reopening the app no longer wipes the route tab for the current run
- Fixed a timing bug where `count == 0` (party transiently empty at ROM load) was clearing restored route encounters immediately after loading them from disk
- Skip the first battle frame when recording wild encounters to avoid capturing stale enemy data from the previous battle at high emulation speeds

## [1.0.3] - 2026-03-14

### Fixed
- AbilityTable was missing Hyper Cutter (52), Pickup (53), and Truant (54) — every ability from Hustle onward was shifted 3 slots too low, causing wrong ability names to display (e.g. Truant shown as Plus).  This has beeen addressed and all tables have been fully compared to the original tracker to ensure this was the only mistake still existing.
- ItemTable corrected several name mismatches vs. Lua tracker source: "Poké Ball", "Poké Doll", "Pokéblock Case", "S.S. Ticket", "Moomoo Milk"

### Changed
- Route tab renamed from "ROUTE" to "ROUTES"
- Routes tab now sorted numerically, with the current route pinned to the top
- "Next Run →" button moved from the tracker header into the emulator Tools popup (middle button) to prevent accidental taps; run counter is incremented when triggered from there
- Opponent sprite shrunk from 80dp to 48dp (matching the player sprite size) to prevent overflow of text to multiple lines.  See below entry as well
- Opponent BST/learnset row moved below the sprite+name header row for more horizontal space

## [1.0.2] - 2026-03-14

### Changed
- BST (Base Stat Total) is now sourced from a static lookup table matching the Ironmon Tracker Lua source (`PokemonData.lua`) instead of being computed by summing individual base stat bytes read from ROM. Previously, hidden information to the player was leaking; this is no longer the case.

## [1.0.1] - 2026-03-14

### Fixed
- Gamepad button capture in settings was completely broken — Compose `AlertDialog` creates its own Android window, so the activity's `dispatchKeyEvent` never fired while the dialog was open. Key events are now handled directly inside the dialog via `Modifier.onKeyEvent`.
- L2 was incorrectly mapped to the GBA R shoulder button (should be R1).

### Added
- X, Y, L2, and R2 controller buttons can now be bound as the speed-trigger button. These buttons are not used for GBA game input, making them ideal for speed control.

## [1.0.0] - 2026-03-12

Initial release of Ironmon Tracker for Android — a merged single-APK combining mGBA emulator and the Ironmon Tracker overlay.

### Features
- 70/30 split layout: game on the left, tracker panel on the right
- Live Pokémon data: species, level, HP bar, nature, ability, types, base stats, shiny/Pokérus/gender indicators
- 3-tab tracker carousel: Main, Stats, Defenses
- Battle panel: enemy species/level/types/revealed moves/weather
- Tap-for-detail sheets: move descriptions, ability info, type defense chart
- Route detection via SaveBlock1 pointer
- Run tracking: run count per ROM, game-over detection, GAME OVER banner
- Game stats: steps, battles, Pokémon Center visits
- Full Gen III support: Fire Red, Leaf Green, Emerald, Ruby, Sapphire
