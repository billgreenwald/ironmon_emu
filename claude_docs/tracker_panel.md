# TrackerPanel

**File:** `tracker/TrackerPanel.kt` (~1233 lines)

## Purpose
Jetpack Compose UI for the 30% right-side tracker panel. Collects `TrackerPoller.state` as Compose state and renders everything.

## Entry Point
```kotlin
@Composable fun TrackerPanel(
    state: TrackerState,
    onQuickload: (() -> Unit)? = null,
    fontScale: Float = 1f,           // scales all sp values; = panelFraction / 0.3f
    isCollapsible: Boolean = false,  // shows ◀/▶ arrow strip + AnimatedVisibility
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
)
```
Added programmatically to `GameActivity` as a `ComposeView` at `leftMargin = gameWidth`.

Font scaling uses `LocalTrackerFontScale` (CompositionLocal) + `@Composable fun ssp(n: Int): TextUnit` helper — all hardcoded sp values in the file call `ssp(N)` instead of `N.sp`.

## Layout Structure
- **Header bar:** ROM title, "Run N" badge, quickload button
- **3-tab carousel** (swipe or tap tabs):
  - **Main:** Party list, HP bar, moves, bag summary, learnset info, route, stats
  - **Stats:** Nature-colored stat bars, IVs, EVs, BST
  - **Defenses:** Type effectiveness grid
- **Battle panel** (overlays bottom when `BattleState.isActive`):
  - Enemy species, level, types, revealed moves, HP bar, weather, side conditions
- **Detail sheets** (modal bottom sheets on tap):
  - `MoveDetailSheet` — power/acc/type/PP + description
  - `AbilityDetailSheet` — ability name + description
  - `TypeDefenseSheet` — full type effectiveness for selected Pokemon

## Key Composables
- `MainView` — main tab content
- `StatsView` — EVs/IVs/nature bars
- `DefensesView` — type chart
- `BattlePanel` — enemy data overlay
- `MoveDetailSheet`, `AbilityDetailSheet`, `TypeDefenseSheet` — bottom sheets

## Compose/Material3 Notes
- Material3 `1.1.0` (Compose BOM 2023.03.00)
- `LinearProgressIndicator(progress = Float, ...)` — NOT lambda form (would require newer BOM)
- Pokemon sprites loaded via Glide (`com.skydoves.landscapist.glide`)
- Tabs use `pagerState` with `HorizontalPager`

## Quickload Flow
`onQuickload` callback → `TrackerPoller.manualNextRun()` → `QuickloadManager.advanceToNext(context)` → `loadRomJNI(nextPath)` → activity restart

## Troubleshooting
- **UI not rendering:** Check ComposeView added in GameActivity.onCreate() + leftMargin set to 70% width
- **Compose crash on LinearProgressIndicator:** Must use `progress = Float` form, not lambda
- **Sprites not loading:** Check Glide dependency in app/build.gradle.kts
- **Tabs not scrolling:** Check `pagerState` + `HorizontalPager` setup
- **Sheet not dismissing:** Modal bottom sheet state management
