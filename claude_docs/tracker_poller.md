# TrackerPoller

**File:** `tracker/TrackerPoller.kt`

## Purpose
The heart of the tracker. 250ms coroutine loop that reads emulator memory and builds `TrackerState`. Also owns game-over detection and run persistence.

## Lifecycle
- `TrackerPoller.start(context, scope)` — called in `GameActivity.onCreate()` after MemoryBridge is set
- `TrackerPoller.stop()` — called in `GameActivity.onDestroy()` before MemoryBridge is cleared
- Runs on `Dispatchers.Default` (background thread)

## Key API
```kotlin
object TrackerPoller {
    val state: StateFlow<TrackerState>
    fun start(context: Context, scope: CoroutineScope)
    fun stop()
    fun resetGameOver()          // manual reset from UI
    fun manualNextRun()          // increment run counter + set game over (quickload path)
}
```

## Poll Loop (`poll()` — called every 250ms)
1. Read ROM game code → detect `GameVersion` + `romVersion` via `GameSettings`
2. Get addresses for that game/version from `DataHelper.addressesFor()`
3. Read party count + decode up to 6 party slots via `PokemonDecoder`
4. Read battle state via `pollBattle()`
5. Read route via `RouteReader`
6. Read game stats via `StatsReader`
7. Read bag via `BagReader`
8. Read learnsets via `LearnsetReader` (lead + enemy species)
9. Detect game over: lead HP == 0 when transitioning from active battle → no battle
10. Persist run attempts via `RunRepository`
11. Track route encounters (encounter slots via `RouteEncounterSlots`)
12. Emit new `TrackerState.Active(...)` to StateFlow

## Battle Poll (`pollBattle()`)
- Reads `battleTypeFlags` — bit 3 = TRAINER (not bit 0!)
- Reads `gBattleMons` slot 1 (enemy) — `BATTLE_MON_SIZE = 0x58` (88 bytes)
- Moves at offset `0x0C` (NOT 0x14)
- Weather from `battleWeather`
- Side conditions (reflect, light screen, safeguard, spikes) from `sideTimers`/`sideStatuses`
- Revealed moves: accumulates enemy moves seen from battle struct each tick

## Game Over Detection
- Lead party slot HP == 0, `isGameOver` flag is set in state
- `manualNextRun()`: increments `runAttempts`, triggers quickload
- Run attempts persisted per ROM code in `filesDir/ironmon_run_<code>.json`

## Dependencies
`MemoryBridge` → `GameSettings` → `DataHelper` → `PokemonDecoder`, `RouteReader`, `StatsReader`, `BagReader`, `LearnsetReader`, `RunRepository`

## Troubleshooting
- **Wrong game detected:** Check `GameSettings.kt` game code sets + ROM_GAME_CODE_ADDR
- **Wrong addresses:** Check `DataHelper.addressesFor()` — verify against Lua tracker source for the specific game/version
- **Battle not detected:** Check `battleTypeFlags` address in DataHelper; bit 3 = trainer flag
- **Wild vs trainer wrong:** Bit 3 of battleTypeFlags must be 0 for wild (common past bug)
- **Run counter not incrementing:** Check `RunRepository` save path + `GameActivity.filesDir`
