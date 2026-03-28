# Changelog

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
