# Ironmon Tracker Android — Architecture Guide

Quick reference for navigating this codebase. See `claude_docs/` for details on each area.

---

## Repo Layout

As of the iOS-port groundwork, the tracker **logic** lives in a Kotlin Multiplatform module
`:tracker-core` (shared, no Android deps). The app module keeps only the Android UI + glue.

```
ironmon_emu/
  mgba-android-memapi/
    android-fork/          ← THE ACTIVE APP (all development happens here)
      tracker-core/        ← KMP MODULE — shared tracker logic (android + jvm + linuxX64 targets)
        src/commonMain/kotlin/hh/game/mgba_android/tracker/
          MemoryBridge.kt   ← native read seam (reader lambda injected per-platform)
          TrackerPoller.kt  ← the poll loop + battle/route/game-over logic (now common)
          models/           ← TrackerState, PokemonData, BattleState, GameVersion
          data/             ← GameSettings, DataHelper, PokemonDecoder + readers
                              (GameStats/BagDetailInfo/LogSource live here too)
          persistence/      ← RunData (@Serializable), RunRepository, ProfileManager,
                              LogRepository, TrackerJson (kotlinx.serialization)
          quickload/        ← RomFamily, RomFamilyGroup, FamilyCache
          tables/           ← SpeciesNames, MoveStatsTable, TypeChart, etc.
          platform/         ← SEAM INTERFACES: Logger/TrackerLog, FileStore, KeyValueStore,
                              AssetReader, LogSource, TrackerSettings, TrackerEnvironment
        src/commonTest/     ← serialization round-trip + game-code detection tests
      app/src/main/
        java/hh/game/mgba_android/
          activity/         ← GameActivity (host for SDL + tracker; builds TrackerEnvironment)
          tracker/          ← ANDROID-ONLY tracker code that stays app-side:
            platform/AndroidPlatform.kt  ← Android impls of the seams + AndroidTrackerPlatform
            TrackerPanel.kt, LogOverlay.kt, GalleryOverlay.kt, NamingOverlay.kt,
            NameEntryButtons.kt, NamingReplayEngine.kt   ← Compose UI (→ SwiftUI on iOS)
            data/LogFileLocator.kt        ← SAF log lookup (Android-specific)
            quickload/QuickloadManager.kt ← bound-service IPC (Android-specific)
        cpp/                ← runGame.cpp (JNI), memapi_server.h, ards.h/cpp
        res/layout/         ← activity_game.xml, padboard.xml
    upstream/               ← Desktop mGBA (reference only, not the Android build)
  Ironmon-Tracker/          ← ORIGINAL LUA TRACKER (read-only reference — always check here first!)
```

### KMP module notes
- **Seams, not `expect/actual`:** platform needs are plain interfaces injected at startup. Android
  builds a `TrackerEnvironment` in `AndroidTrackerPlatform.install()` and passes it to
  `TrackerPoller.start(environment, scope, romPath)`. iOS will supply its own impls.
- **`linuxX64` is a validation-only target** (never shipped): it compiles `commonMain` through
  Kotlin/Native *on Linux*, catching iOS-incompatible code (java.* APIs, JVM `@Volatile`) without
  a Mac. Run `:tracker-core:compileKotlinLinuxX64` / `:tracker-core:linuxX64Test` as the iOS gate.
- **Persistence wire format preserved:** the Gson→kotlinx.serialization move keeps identical JSON
  field names so existing `ironmon_run_*.json` device saves load unchanged.
- Repositories now take injected stores, not `Context`: `RunRepository.load(fileStore, id)`,
  `FamilyCache.load(fileStore)`, `LogRepository.getLog(logSource, …)`.

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
| Trainer counts wrong / missing | Vanilla: `TrainerFlagReader.kt` + `TrainerRouteTable.kt` (flag bits). MaxFR: `TrackerPoller.trainerDefeatsByRoute` (battle-event tracking, persisted in `RunData.trainerDefeatsByRoute`) | [`data_readers.md`](claude_docs/data_readers.md) |
| Wrong bag items/quantities | `BagReader.kt` + XOR decrypt | [`data_readers.md`](claude_docs/data_readers.md) |
| Wrong learnset moves | `LearnsetReader.kt` + `levelUpLearnsets` address | [`data_readers.md`](claude_docs/data_readers.md) |
| UI not rendering / layout broken | `TrackerPanel.kt` + `activity_game.xml` | [`tracker_panel.md`](claude_docs/tracker_panel.md), [`game_activity_layout.md`](claude_docs/game_activity_layout.md) |
| Compose crash | Material3 1.1.0 API compat (LinearProgressIndicator) | [`tracker_panel.md`](claude_docs/tracker_panel.md) |
| Quickload not working | `QuickloadManager.kt` + `loadRomJNI` in `runGame.cpp` | [`quickload.md`](claude_docs/quickload.md) |
| Vanilla mode not randomizing | UPR-Android (`ly.mens.rndpkmn`) must be installed; check `randomizeCurrentRom()` in `QuickloadManager.kt` | [`quickload.md`](claude_docs/quickload.md) |
| Run count not saving | `RunRepository.kt` / `filesDir` | [`persistence.md`](claude_docs/persistence.md) |
| Non-English ROM not detected | `GameSettings.kt` game code sets | [`data_addresses.md`](claude_docs/data_addresses.md) |
| Build failure | NDK version, Gradle path, Material3 version | [`build_system.md`](claude_docs/build_system.md) |
| Wrong ability/nature name | `AbilityTable.kt` / `NatureTable.kt` | [`lookup_tables.md`](claude_docs/lookup_tables.md) |
| Type effectiveness wrong | `TypeChart.kt` — indexed by ROM type IDs (not 0-based!) | [`lookup_tables.md`](claude_docs/lookup_tables.md) |
| Star rating wrong / missing | `GachaMonRating.kt` + `AbilityRatingTable.kt` + `MoveRatingTable.kt`; source of truth is `GachaMonRatingSystem.json` in the Lua tracker | |

---

## Data Flow (top to bottom)

Everything from `MemoryBridge` down lives in the shared `:tracker-core` module; only the JNI
source and the Compose UI are Android-specific.

```
JNI: getMemoryRange(addr, len) → ByteArray   [Android: runGame.cpp / iOS: Swift bridge]
         ↓
    MemoryBridge          (thin wrapper, reader lambda injected by the host platform)
         ↓
    TrackerPoller         (250ms coroutine — reads everything, emits state;
                           platform needs injected via TrackerEnvironment)
    ├── GameSettings      (ROM detection: 0x080000AC)
    ├── DataHelper        (per-game addresses)
    ├── PokemonDecoder    (XOR decrypt + 24 orderings)
    ├── GachaMonRating    (star rating: ability + moves + stats + nature, per ruleset)
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
