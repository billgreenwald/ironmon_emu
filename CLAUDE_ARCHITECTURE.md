# Ironmon Tracker Android — Architecture Guide

Quick reference for navigating this codebase. See `claude_docs/` for details on each area.

---

## Repo Layout

```
ironmon_emu/
  mgba-android-memapi/
    android-fork/          ← THE ACTIVE APP (all development happens here)
      app/src/main/
        java/hh/game/mgba_android/
          activity/         ← GameActivity (host for SDL + tracker)
          tracker/          ← ALL tracker Kotlin code
            MemoryBridge.kt
            TrackerPoller.kt
            TrackerPanel.kt
            models/         ← TrackerState, PokemonData, BattleState, GameVersion
            data/           ← GameSettings, DataHelper, PokemonDecoder + readers
            persistence/    ← RunData, RunRepository, ProfileManager
            quickload/      ← RomFamily, FamilyCache, QuickloadManager
            tables/         ← SpeciesNames, MoveStatsTable, TypeChart, etc.
        cpp/                ← runGame.cpp (JNI), memapi_server.h, ards.h/cpp
        res/layout/         ← activity_game.xml, padboard.xml
    upstream/               ← Desktop mGBA (reference only, not the Android build)
  Ironmon-Tracker/          ← ORIGINAL LUA TRACKER (read-only reference — always check here first!)
```

---

## When to Check What

| Problem | Where to look | Doc |
|---------|--------------|-----|
| Wrong memory addresses | `DataHelper.kt` → verify against Lua tracker | [`data_addresses.md`](claude_docs/data_addresses.md) |
| Wrong species/stats/moves decoded | `PokemonDecoder.kt` | [`pokemon_decoder.md`](claude_docs/pokemon_decoder.md) |
| Memory reads returning null | `MemoryBridge.kt` + `GameActivity.onCreate()` | [`memory_bridge.md`](claude_docs/memory_bridge.md) |
| Battle not detected / wrong type | `TrackerPoller.pollBattle()`, `DataHelper battleTypeFlags` | [`tracker_poller.md`](claude_docs/tracker_poller.md) |
| Wild vs trainer wrong | `battleTypeFlags` bit 3 = TRAINER (not bit 0!) | [`tracker_poller.md`](claude_docs/tracker_poller.md) |
| Game over not triggering | `TrackerPoller` lead HP check | [`tracker_poller.md`](claude_docs/tracker_poller.md) |
| Wrong route name | `RouteReader.kt` + `RouteNames.kt` | [`data_readers.md`](claude_docs/data_readers.md) |
| Wrong steps/battles/centers | `StatsReader.kt` + XOR decrypt | [`data_readers.md`](claude_docs/data_readers.md) |
| Trainer counts wrong / missing | `TrainerFlagReader.kt` + `TrainerRouteTable.kt`; verify `gameFlagsOffset` in `DataHelper` | [`data_readers.md`](claude_docs/data_readers.md) |
| Wrong bag items/quantities | `BagReader.kt` + XOR decrypt | [`data_readers.md`](claude_docs/data_readers.md) |
| Wrong learnset moves | `LearnsetReader.kt` + `levelUpLearnsets` address | [`data_readers.md`](claude_docs/data_readers.md) |
| UI not rendering / layout broken | `TrackerPanel.kt` + `activity_game.xml` | [`tracker_panel.md`](claude_docs/tracker_panel.md), [`game_activity_layout.md`](claude_docs/game_activity_layout.md) |
| Compose crash | Material3 1.1.0 API compat (LinearProgressIndicator) | [`tracker_panel.md`](claude_docs/tracker_panel.md) |
| Quickload not working | `QuickloadManager.kt` + `loadRomJNI` in `runGame.cpp` | [`quickload.md`](claude_docs/quickload.md) |
| Run count not saving | `RunRepository.kt` / `filesDir` | [`persistence.md`](claude_docs/persistence.md) |
| Non-English ROM not detected | `GameSettings.kt` game code sets | [`data_addresses.md`](claude_docs/data_addresses.md) |
| Build failure | NDK version, Gradle path, Material3 version | [`build_system.md`](claude_docs/build_system.md) |
| Wrong ability/nature name | `AbilityTable.kt` / `NatureTable.kt` | [`lookup_tables.md`](claude_docs/lookup_tables.md) |
| Type effectiveness wrong | `TypeChart.kt` — indexed by ROM type IDs (not 0-based!) | [`lookup_tables.md`](claude_docs/lookup_tables.md) |

---

## Data Flow (top to bottom)

```
JNI: getMemoryRange(addr, len) → ByteArray
         ↓
    MemoryBridge          (thin wrapper, reader set by GameActivity)
         ↓
    TrackerPoller         (250ms coroutine — reads everything, emits state)
    ├── GameSettings      (ROM detection: 0x080000AC)
    ├── DataHelper        (per-game addresses)
    ├── PokemonDecoder    (XOR decrypt + 24 orderings)
    ├── RouteReader       (gMapHeader → RouteNames)
    ├── StatsReader       (SaveBlock1 XOR decrypt → steps/battles/centers)
    ├── BagReader         (SaveBlock1 items/berries pockets)
    └── LearnsetReader    (ROM level-up move table)
         ↓
    StateFlow<TrackerState>
         ↓
    TrackerPanel          (Compose UI — 3-tab carousel + battle panel + sheets)
```

---

## Cross-Game Support

This tracker supports 5 games. **Never hardcode Emerald-only addresses.**

| Game | Status | Notes |
|------|--------|-------|
| Fire Red (EN v1.0/v1.1) | ✅ Verified | Different `baseStatsTable` per version |
| Leaf Green (EN v1.0/v1.1) | ✅ Verified | Same structure as FR |
| Fire Red/Leaf Green non-EN | ✅ Verified | Different `saveBlock2Ptr` per language |
| Emerald | ✅ Verified | Single version, XOR-encrypted |
| Ruby/Sapphire | ⚠️ Partial | Battle addresses are **UNVERIFIED PLACEHOLDERS** |

---

## Golden Rules

1. **Always check the Lua tracker first** (`Ironmon-Tracker/` directory) before touching any memory address. This is the source of truth for constants.
2. **All 5 games must work** — addresses live in `DataHelper.addressesFor()`, never inline.
3. **ROM type IDs are not 0-based** — type 11=Fire, 12=Water, etc. Don't remap before lookup.
4. **Battle type detection:** bit 3 of `battleTypeFlags` = TRAINER (bit 3 == 0 means wild).
5. **SaveBlock1 is a pointer in FR/LG/Emerald** — must dereference; Ruby/Sapphire is direct.

---

## Detailed Docs

- [`claude_docs/memory_bridge.md`](claude_docs/memory_bridge.md) — JNI memory bridge
- [`claude_docs/tracker_poller.md`](claude_docs/tracker_poller.md) — 250ms poll loop
- [`claude_docs/tracker_panel.md`](claude_docs/tracker_panel.md) — Compose UI
- [`claude_docs/data_addresses.md`](claude_docs/data_addresses.md) — memory addresses + struct layouts
- [`claude_docs/pokemon_decoder.md`](claude_docs/pokemon_decoder.md) — XOR decrypt + substructures
- [`claude_docs/data_readers.md`](claude_docs/data_readers.md) — RouteReader, StatsReader, BagReader, LearnsetReader
- [`claude_docs/models.md`](claude_docs/models.md) — TrackerState, PokemonData, BattleState
- [`claude_docs/persistence.md`](claude_docs/persistence.md) — RunData, RunRepository, ProfileManager
- [`claude_docs/quickload.md`](claude_docs/quickload.md) — ROM family + quickload system
- [`claude_docs/lookup_tables.md`](claude_docs/lookup_tables.md) — all static data tables
- [`claude_docs/game_activity_layout.md`](claude_docs/game_activity_layout.md) — GameActivity + 70/30 layout
- [`claude_docs/build_system.md`](claude_docs/build_system.md) — build commands + environment
