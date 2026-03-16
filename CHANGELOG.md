# Changelog

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
