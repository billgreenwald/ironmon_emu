# Changelog

## [1.0.3] - 2026-03-14

### Fixed
- AbilityTable was missing Hyper Cutter (52), Pickup (53), and Truant (54) — every ability from Hustle onward was shifted 3 slots too low, causing wrong ability names to display (e.g. Truant shown as Plus)
- ItemTable corrected several name mismatches vs. Lua tracker source: "Poké Ball", "Poké Doll", "Pokéblock Case", "S.S. Ticket", "Moomoo Milk"

### Changed
- Route tab renamed from "ROUTE" to "ROUTES"
- Routes tab now sorted numerically, with the current route pinned to the top
- "Next Run →" button moved from the tracker header into the emulator Tools popup (middle button) to prevent accidental taps; run counter is incremented when triggered from there
- Opponent sprite shrunk from 80dp to 48dp (matching the player sprite size)
- Opponent BST/learnset row moved below the sprite+name header row for more horizontal space

## [1.0.2] - 2026-03-14

### Changed
- BST (Base Stat Total) is now sourced from a static lookup table matching the Ironmon Tracker Lua source (`PokemonData.lua`) instead of being computed by summing individual base stat bytes read from ROM. This eliminates 6 ROM reads per Pokémon per poll cycle and ensures BST values are always consistent with the reference tracker.

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
