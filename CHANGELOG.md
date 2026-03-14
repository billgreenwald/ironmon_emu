# Changelog

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
