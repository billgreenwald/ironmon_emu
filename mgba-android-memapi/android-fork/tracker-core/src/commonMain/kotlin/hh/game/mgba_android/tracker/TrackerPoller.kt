package hh.game.mgba_android.tracker

import hh.game.mgba_android.tracker.platform.TrackerEnvironment
import hh.game.mgba_android.tracker.platform.TrackerLog
import kotlinx.atomicfu.atomic
import hh.game.mgba_android.tracker.data.BagDetailInfo
import hh.game.mgba_android.tracker.data.BagReader
import hh.game.mgba_android.tracker.data.DataHelper
import hh.game.mgba_android.tracker.data.GachaMonRating
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.GameOverCondition
import hh.game.mgba_android.tracker.data.GameAddresses
import hh.game.mgba_android.tracker.data.GameSettings
import hh.game.mgba_android.tracker.data.MaxFrAddressLoader
import hh.game.mgba_android.tracker.data.MaxFrVariant
import hh.game.mgba_android.tracker.data.NatDexEra
import hh.game.mgba_android.tracker.data.NatDexMetaAddressReader
import hh.game.mgba_android.tracker.data.LearnsetReader
import hh.game.mgba_android.tracker.data.PokemonDecoder
import hh.game.mgba_android.tracker.data.RouteReader
import hh.game.mgba_android.tracker.data.StatsReader
import hh.game.mgba_android.tracker.data.TrainerFlagReader
import hh.game.mgba_android.tracker.tables.CombinedAreas
import hh.game.mgba_android.tracker.tables.TrainerRouteTable
import hh.game.mgba_android.tracker.models.BattleState
import hh.game.mgba_android.tracker.models.EnemyData
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.TrackedMove
import hh.game.mgba_android.tracker.models.TrackerState
import hh.game.mgba_android.tracker.models.Weather
import hh.game.mgba_android.tracker.persistence.RunRepository
import hh.game.mgba_android.tracker.tables.AbilityTable
import hh.game.mgba_android.tracker.tables.BstTable
import hh.game.mgba_android.tracker.tables.MoveNames
import hh.game.mgba_android.tracker.tables.SpeciesNames
import kotlin.concurrent.Volatile
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TrackerPoller {

    private val _state = MutableStateFlow<TrackerState>(TrackerState.Disconnected)
    val state: StateFlow<TrackerState> = _state

    @Volatile var currentAddresses: GameAddresses? = null
        private set
    @Volatile private var isNatDex: Boolean = false
    @Volatile private var natDexEra: NatDexEra = NatDexEra.NONE
    // NatDex 1.2.x+ addresses read from the ROM meta-table (null for 1.1.x / non-NatDex).
    @Volatile private var cachedNatDexAddresses: GameAddresses? = null
    @Volatile private var maxFrVariant: MaxFrVariant = MaxFrVariant.NONE
    // Cached MaxFR GameAddresses — loaded once per ROM via JSON asset, cleared on game code change.
    @Volatile private var cachedMaxFrAddresses: GameAddresses? = null

    // Persistent move store: key = speciesId only (Lua: allPokemon[pokemonID].moves — flat, unbounded)
    private val revealedBySpecies = mutableMapOf<Int, MutableList<TrackedMove>>()
    // Ephemeral per-battle notes: key = speciesId*1000+level (Lua: Tracker.BattleNotes)
    private val battleRevealedByKey = mutableMapOf<Long, MutableList<Int>>()
    private var battleFourConfirmedKey: Long? = null  // set when all 4 confirmed this battle
    // Starting enemy moveset snapshot at battle open — guards Sketch/Mimic (Lua BattleParties)
    private var enemyStartingMoveset: Set<Int> = emptySet()
    private var lastEnemyMoveId: Int = 0     // last value of gBattleResults+0x24 (shared between both enemies)
    private var lastKnownEnemySpeciesId: Int = 0  // detects mid-battle trainer switches
    private var battleJustStarted: Boolean = false
    private var lastBattleActive = false
    // ── Doubles: enemy2 (gBattleMons slot 3) tracking ─────────────────────────
    private val battleRevealedByKey2 = mutableMapOf<Long, MutableList<Int>>()
    private var battleFourConfirmedKey2: Long? = null
    private var enemy2StartingMoveset: Set<Int> = emptySet()
    private var lastKnownEnemy2SpeciesId: Int = 0

    // Route encounter tracking: mapLayoutId → species IDs seen (wild battles only)
    private val encountersByRoute = mutableMapOf<Int, MutableList<Int>>()
    private val routeVisitOrder = mutableListOf<Int>()  // insertion-ordered unique mapLayoutIds
    private val visitedRoutes = mutableSetOf<Int>()  // every map the player has stepped onto
    // Last mapLayoutId we logged, so ROUTE_CHANGE fires only when the map actually changes
    // (not every 250ms poll). Aids debugging "Unknown Location" — correlate with USER_FLAG lines.
    private var lastLoggedMapId: Int? = null
    // Whether the current wild battle has already been recorded (reset when battle ends)
    private var currentWildBattleRecorded = false

    // Trainer defeat tracking (MaxFR variants): mapLayoutId → defeats this run
    // Used instead of flag-based TrainerFlagReader for ROM hacks where trainer IDs may differ.
    private val trainerDefeatsByRoute = mutableMapOf<Int, Int>()
    // Track wild/trainer type of the ACTIVE battle so we can check it at battle-end.
    // battleRaw.isWild is unreliable at end (pollBattle returns NONE when inactive).
    private var lastBattleWasWild = false
    // Two-trainer double battle (Emerald optional double) of the ACTIVE battle — captured
    // while active so we can credit BOTH defeated trainers at battle-end.
    private var lastBattleWasTwoTrainerDouble = false

    // Ball picker: random starter position (1=Left, 2=Middle, 3=Right; 0=unset)
    private val chosenBall = atomic(0)

    /** Re-randomize the ball picker (user pressed Reroll). */
    fun rerollBall() { chosenBall.value = Random.nextInt(1, 4) }

    // Game over state — atomic so compareAndSet prevents double-increment
    // if two poll coroutines both see battle-end before either sets the flag
    private val isGameOver = atomic(false)

    // Catching tutorial state — mirrors Lua Program.updateCatchingTutorial / Battle.recentBattleWasTutorial
    private var recentBattleWasTutorial = false
    private var hasCompletedTutorial = false

    // Run persistence + platform capabilities (injected via start(); keeps this class common)
    private var env: TrackerEnvironment? = null
    private var lastGameCode: String = ""
    private var romFamilyKey: String = ""  // derived from ROM filename prefix; key for RunRepository
    @Volatile private var romFileName: String = ""  // full ROM filename (with ext); used to locate the .log
    private val logAvailable = atomic(false)  // set once when a matching .log is found next to the ROM

    /** Full filename (with extension) of the loaded ROM, for locating its randomizer log. */
    fun getRomFileName(): String = romFileName
    @Volatile private var runAttempts: Int = 0
    // Per-species notes for current run (keyed by species ID, cleared on new run)
    private val pokemonNotesCache = mutableMapOf<Int, String>()

    fun resetGameOver() {
        isGameOver.value = false
        revealedBySpecies.clear()
        battleRevealedByKey.clear()
        battleFourConfirmedKey = null
        enemyStartingMoveset = emptySet()
        lastEnemyMoveId = 0
        lastKnownEnemySpeciesId = 0
        battleRevealedByKey2.clear()
        battleFourConfirmedKey2 = null
        enemy2StartingMoveset = emptySet()
        lastKnownEnemy2SpeciesId = 0
        encountersByRoute.clear()
        routeVisitOrder.clear()
        visitedRoutes.clear()
        currentWildBattleRecorded = false
        pokemonNotesCache.clear()
        trainerDefeatsByRoute.clear()
        // Clear persisted route encounters + notes + trainer defeats for new run
        env?.let { ctx ->
            if (romFamilyKey.isNotEmpty()) {
                val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                data.routeEncounters.clear()
                data.visitedRoutes.clear()
                data.pokemonNotes.clear()
                data.trainerDefeatsByRoute.clear()
                RunRepository.save(ctx.fileStore, romFamilyKey, data)
            }
        }
    }
    fun debugForceGameOver() { isGameOver.value = true }

    fun setRunAttempts(n: Int) {
        runAttempts = n
        val ctx = env ?: return
        if (romFamilyKey.isEmpty()) return
        val data = RunRepository.load(ctx.fileStore, romFamilyKey)
        data.stats.attempts = n
        RunRepository.save(ctx.fileStore, romFamilyKey, data)
    }

    /** Called when the user triggers "Next Run" (banner or tools menu).
     *  Only increments if game-over detection hasn't already done so.
     *  Always clears routes/moves and resets the game-over flag for the new run. */
    fun manualNextRun() {
        // getAndSet(true) returns the old value.
        // If it was false, we own this transition — increment.
        // If it was already true (died normally), detection already incremented — skip.
        if (!isGameOver.getAndSet(true)) {
            runAttempts++
            val ctx = env ?: return
            if (romFamilyKey.isEmpty()) return
            val data = RunRepository.load(ctx.fileStore, romFamilyKey)
            data.stats.attempts = runAttempts
            RunRepository.save(ctx.fileStore, romFamilyKey, data)
        }
        // Always clear route encounters and revealed moves for the new run,
        // and reset isGameOver so the next death can be detected.
        resetGameOver()
    }

    /** Save a per-species note for the current run. Empty string removes the note. */
    fun saveNote(speciesId: Int, note: String) {
        val trimmed = note.trim()
        if (trimmed.isEmpty()) pokemonNotesCache.remove(speciesId)
        else pokemonNotesCache[speciesId] = trimmed
        env?.let { ctx ->
            if (romFamilyKey.isNotEmpty()) {
                val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                data.pokemonNotes.clear()
                data.pokemonNotes.putAll(pokemonNotesCache)
                RunRepository.save(ctx.fileStore, romFamilyKey, data)
            }
        }
        _state.update { s -> if (s is TrackerState.Active) s.copy(pokemonNotes = pokemonNotesCache.toMap()) else s }
    }

    private var pollJob: Job? = null

    fun start(environment: TrackerEnvironment, scope: CoroutineScope, romPath: String? = null) {
        env = environment
        if (romPath != null) {
            val fileName = romPath.substringAfterLast('/')
            romFileName = fileName
            romFamilyKey = hh.game.mgba_android.tracker.quickload.RomFamilyUtils.parseFamily(fileName, romPath).prefix
            // One-time probe: is there a randomizer .log next to this ROM? Gates the
            // "Review Logs" banner. Off the main thread since the log lookup is slow.
            logAvailable.value = false
            if (fileName.isNotEmpty()) {
                scope.launch(Dispatchers.Default) {
                    logAvailable.value = environment.logSource.exists(fileName)
                }
            }
        }
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                _state.value = poll()
                delay(if (lastBattleActive) BATTLE_POLL_INTERVAL_MS else POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = TrackerState.Disconnected
        revealedBySpecies.clear()
        battleRevealedByKey.clear()
        battleFourConfirmedKey = null
        enemyStartingMoveset = emptySet()
        lastEnemyMoveId = 0
        lastKnownEnemySpeciesId = 0
        battleRevealedByKey2.clear()
        battleFourConfirmedKey2 = null
        enemy2StartingMoveset = emptySet()
        lastKnownEnemy2SpeciesId = 0
        encountersByRoute.clear()
        routeVisitOrder.clear()
        visitedRoutes.clear()
        currentWildBattleRecorded = false
        pokemonNotesCache.clear()
        lastGameCode = ""  // force restore from disk on next game load
        isNatDex = false
        natDexEra = NatDexEra.NONE
        cachedNatDexAddresses = null
        maxFrVariant = MaxFrVariant.NONE
        cachedMaxFrAddresses = null
        hh.game.mgba_android.tracker.data.MaxExtDataStore.clear()
        recentBattleWasTutorial = false
        hasCompletedTutorial = false
    }

    private fun poll(): TrackerState {
        if (MemoryBridge.reader == null) return TrackerState.Disconnected

        val codeBytes = MemoryBridge.readBytes(GameSettings.ROM_GAME_CODE_ADDR, 4)
            ?: return TrackerState.Disconnected

        val game = GameSettings.detectGame(codeBytes)
        if (game == GameVersion.UNKNOWN) return TrackerState.NoGameLoaded

        // Read ROM version byte (0=v1.0, 1=v1.1, 2=v1.2) and 4-char game code string
        val romVersion = GameSettings.readVersionByte { addr, len -> MemoryBridge.readBytes(addr, len) }
        val gameCode = codeBytes.take(4).map { (it.toInt() and 0xFF).toChar() }.joinToString("")
        val romTitle = MemoryBridge.readBytes(0x080000A0L, 12)
            ?.let { b -> b.map { (it.toInt() and 0xFF).toChar() }.joinToString("").trimEnd('\u0000', ' ') } ?: ""

        // ── NatDex / MaxFR detection: run once per ROM load (game code change) ─────────────────────
        if (gameCode != lastGameCode) {
            natDexEra = GameSettings.detectNatDexEra { addr, len -> MemoryBridge.readBytes(addr, len) }
            isNatDex = natDexEra != NatDexEra.NONE
            // 1.2.x+: addresses come from the ROM meta-table (version-proof). 1.1.x uses DataHelper.
            cachedNatDexAddresses = if (natDexEra == NatDexEra.V12) {
                NatDexMetaAddressReader.read { addr, len -> MemoryBridge.readBytes(addr, len) }
            } else null
            val environment = env
            if (!isNatDex && environment != null && game in setOf(
                    GameVersion.EMERALD, GameVersion.FIRE_RED, GameVersion.LEAF_GREEN)) {
                val detected = MaxFrAddressLoader.detectAndLoad(environment.assetReader) { addr, len -> MemoryBridge.readBytes(addr, len) }
                maxFrVariant = detected?.first ?: MaxFrVariant.NONE
                cachedMaxFrAddresses = detected?.second
            } else {
                maxFrVariant = MaxFrVariant.NONE
                cachedMaxFrAddresses = null
            }
            TrackerLog.d(TAG, "natDexEra=$natDexEra isNatDex=$isNatDex maxFrVariant=$maxFrVariant for gameCode=$gameCode")
        }

        // V12 meta-table may not be readable on the very first tick (core not ready); retry until it is.
        if (natDexEra == NatDexEra.V12 && cachedNatDexAddresses == null) {
            cachedNatDexAddresses = NatDexMetaAddressReader.read { addr, len -> MemoryBridge.readBytes(addr, len) }
        }

        val addresses = when {
            cachedMaxFrAddresses != null  -> cachedMaxFrAddresses!!
            cachedNatDexAddresses != null -> cachedNatDexAddresses!!
            // V12 but the meta-table isn't readable yet — wait rather than fall back to wrong (1.1.x) addresses.
            natDexEra == NatDexEra.V12    -> return TrackerState.NoGameLoaded
            else -> DataHelper.addressesFor(game, romVersion, gameCode, isNatDex)
                ?: return TrackerState.NoGameLoaded
        }
        currentAddresses = addresses

        // ── Run persistence: load attempt count + route encounters when game code changes ─────────
        if (gameCode != lastGameCode) {
            lastGameCode = gameCode
            chosenBall.value = 0  // new ROM — reset ball picker
            TrackerLog.d(TAG, "gameCode changed to $gameCode — restoring run data")
            env?.let { ctx ->
                val runData = RunRepository.load(ctx.fileStore, romFamilyKey)
                runAttempts = runData.stats.attempts
                // Restore notes from saved data
                pokemonNotesCache.clear()
                pokemonNotesCache.putAll(runData.pokemonNotes)
                // Restore encounter map from saved data
                encountersByRoute.clear()
                routeVisitOrder.clear()
                visitedRoutes.clear()
                runData.routeEncounters.forEach { (mapIdStr, species) ->
                    val mapId = mapIdStr.toIntOrNull() ?: return@forEach
                    encountersByRoute[mapId] = species.map { (it as? Number)?.toInt() ?: it as Int }.toMutableList()
                    TrackerLog.d(TAG, "restore mapId=$mapId species=${encountersByRoute[mapId]}")
                }
                encountersByRoute.keys.sorted().forEach { routeVisitOrder.add(it) }
                visitedRoutes.addAll(runData.visitedRoutes)
                // Restore trainer defeats for MaxFR session tracking
                trainerDefeatsByRoute.clear()
                runData.trainerDefeatsByRoute.forEach { (mapIdStr, count) ->
                    mapIdStr.toIntOrNull()?.let { trainerDefeatsByRoute[it] = count }
                }
                TrackerLog.d(TAG, "restore done: encountersByRoute=$encountersByRoute visitedRoutes=$visitedRoutes trainerDefeatsByRoute=$trainerDefeatsByRoute")
            } ?: run { runAttempts = 0 }
        }

        // ── Party ─────────────────────────────────────────────────────────────
        val partyCount = MemoryBridge.readU8(addresses.partyCount)
            ?: return TrackerState.Disconnected
        val count = partyCount.coerceIn(0, 6)

        val party = buildList {
            for (slot in 0 until count) {
                val slotAddr = addresses.partyBase + slot * DataHelper.POKEMON_STRUCT_SIZE
                val raw = MemoryBridge.readBytes(slotAddr, DataHelper.POKEMON_STRUCT_SIZE)
                    ?: continue
                val pokemon = PokemonDecoder.decode(
                    slot      = slot,
                    raw       = raw,
                    nameTable = { id -> addresses.extPokemonMap[id]?.name ?: SpeciesNames.get(id) },
                    moveTable = { id -> MoveNames.get(id, maxFrVariant != MaxFrVariant.NONE) },
                    baseStatsReader = { speciesId ->
                        val addr = addresses.baseStatsTable +
                            speciesId * DataHelper.BASE_STATS_ENTRY_SIZE
                        MemoryBridge.readBytes(addr, DataHelper.BASE_STATS_ENTRY_SIZE)
                    },
                    bstLookup = { id -> addresses.extPokemonMap[id]?.bst ?: BstTable.bst(id) },
                    isMaxFr = maxFrVariant != MaxFrVariant.NONE,
                    isNatDex = isNatDex,
                    moveStatsReader = if (addresses.gBattleMoves != 0L) { moveId ->
                        DataHelper.readMoveStatsFromRom(
                            { addr, len -> MemoryBridge.readBytes(addr, len) },
                            addresses.gBattleMoves,
                            moveId,
                        )
                    } else null,
                )
                if (pokemon != null) {
                    val spriteId = addresses.extPokemonMap[pokemon.speciesId]?.spriteId ?: pokemon.speciesId
                    val withNatDex = if (spriteId != pokemon.speciesId) pokemon.copy(natDexId = spriteId) else pokemon
                    val rated = if (slot == 0) {
                        val ruleset = env?.settings?.getRuleset() ?: GachaMonRuleset.STANDARD
                        val score = GachaMonRating.calculateRatingScore(withNatDex, ruleset)
                        val stars = GachaMonRating.calculateStars(score)
                        withNatDex.copy(ratingScore = score, starRating = stars)
                    } else withNatDex
                    add(rated)
                }
            }
        }

        // ── Per-move category (MaxFR/MaxEM: Gen IV physical/special split read from ROM) ──────────
        val moveCategories: Map<Int, Int> = if (addresses.gBattleMoves != 0L) {
            party.flatMap { it.moves }.map { it.moveId }.distinct()
                .associateWith { id -> DataHelper.readMoveCategory({ addr, len -> MemoryBridge.readBytes(addr, len) }, addresses.gBattleMoves, id) }
                .filter { it.value != 0 }
        } else emptyMap()

        // ── Route ─────────────────────────────────────────────────────────────
        val route = RouteReader.read(game, addresses)

        // Track visited routes — record the moment the player steps onto a new map
        val mapId = route?.mapLayoutId
        // Log every map transition (raw layout id + resolved name) so a developer can pin down
        // "Unknown Location" ids from an exported log — correlate with USER_FLAG demarcations.
        if (mapId != lastLoggedMapId) {
            lastLoggedMapId = mapId
            TrackerLog.d("ROUTE_CHANGE", "mapLayoutId=$mapId name=${route?.name} game=$game")
        }
        if (mapId != null && mapId !in visitedRoutes) {
            visitedRoutes.add(mapId)
            env?.let { ctx ->
                val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                if (mapId !in data.visitedRoutes) {
                    data.visitedRoutes.add(mapId)
                    RunRepository.save(ctx.fileStore, romFamilyKey, data)
                }
            }
        }

        // ── Ball picker (mirrors Lua TrackerScreen.canShowBallPicker) ────────────────────────
        // IsInLab mapLayoutIds from Lua RouteData.Locations.IsInLab: FRLG=5, RSE/Emerald=17
        val starterLabLayoutId = when (game) {
            GameVersion.FIRE_RED, GameVersion.LEAF_GREEN -> 5
            else -> 17  // Ruby, Sapphire, Emerald — Route 101 / Birch's lab
        }
        val inStarterLab = mapId == starterLabLayoutId
        when {
            count == 0 && inStarterLab -> if (chosenBall.value == 0) chosenBall.value = Random.nextInt(1, 4)
            count > 0 -> chosenBall.value = 0  // player chose a starter — dismiss picker
        }
        val showBallPicker = count == 0 && inStarterLab

        // ── Catching tutorial detection (mirrors Lua Program.updateCatchingTutorial) ──────────
        // sSpecialFlags == 3 while the tutorial battle is active; drops to 0 when done.
        if (!hasCompletedTutorial && addresses.sSpecialFlags != 0L) {
            val tutorialFlag = MemoryBridge.readU8(addresses.sSpecialFlags) ?: 0
            when {
                tutorialFlag == 3 -> recentBattleWasTutorial = true
                recentBattleWasTutorial && tutorialFlag == 0 -> hasCompletedTutorial = true
            }
        }

        // ── Battle ────────────────────────────────────────────────────────────
        // Capture pre-poll battle state so we can detect the active→ended transition below.
        val wasBattleActive = lastBattleActive
        val battleRaw0 = pollBattle(game, addresses)

        // Which party slot is the player's ACTIVE on-field mon. gBattleMons slot 0 tracks the
        // active battler, not necessarily party slot 0 — after a mid-battle switch this differs.
        // Out of battle it defaults to 0 (party lead). Mirrors Lua Battle.Combatants.LeftOwn.
        val activeIdx = if (battleRaw0.isActive && party.isNotEmpty())
            battleRaw0.playerMon1PartyIdx.coerceIn(0, party.lastIndex) else 0

        // Only trust live gBattleMons slot-0 reads (active mon's live types + stat stages) when
        // slot 0 actually holds the active party mon. On some ROM hacks (Max variants) the player
        // slot reads all zeros in battle, which corrupted the type (→ Normal) and stat stages
        // (→ all -6). Validate the slot-0 species against the active party mon.
        val slot0Species = if (battleRaw0.isActive) MemoryBridge.readU16(addresses.battleMons) ?: 0 else 0
        val playerSlotValid = battleRaw0.isActive && party.isNotEmpty() && slot0Species == party.getOrNull(activeIdx)?.speciesId
        val battleRaw = if (battleRaw0.isActive && !playerSlotValid)
            battleRaw0.copy(playerType1 = -1, playerType2 = -1, playerStatStages = null)
        else battleRaw0

        // Override the active Pokémon's types with live gBattleMons types while in battle.
        // This reflects Conversion, Conversion 2, Camouflage, and Color Change automatically.
        val liveParty = if (battleRaw.isActive && battleRaw.playerType1 in 0..18 && party.isNotEmpty()) {
            party.mapIndexed { i, p ->
                if (i == activeIdx) p.copy(type1 = battleRaw.playerType1, type2 = battleRaw.playerType2) else p
            }
        } else party

        // Reset tutorial flag at the start of each new battle (mirrors Lua Battle init reset)
        if (!wasBattleActive && battleRaw.isActive) recentBattleWasTutorial = false
        // Snapshot wild/trainer type while the battle is active (isWild is invalid in BattleState.NONE)
        if (battleRaw.isActive) {
            lastBattleWasWild = battleRaw.isWild
            lastBattleWasTwoTrainerDouble = battleRaw.isTwoTrainerDouble
        }

        // ── Wild encounter recording (Lua tracker: Tracker.TrackRouteEncounter) ───
        // Reset flag when battle ends so it's ready for the next encounter.
        if (!battleRaw.isActive) currentWildBattleRecorded = false
        // Skip the very first active frame — gBattleMons[1] may still hold stale enemy data
        // from the previous battle (especially at 3x speed). Wait one extra poll cycle (250ms).
        val isFirstBattleFrame = !wasBattleActive && battleRaw.isActive
        if (battleRaw.isActive && battleRaw.isWild && !currentWildBattleRecorded && !isFirstBattleFrame) {
            val encounterMapId = route?.mapLayoutId
            val sid = battleRaw.enemy?.speciesId
            if (encounterMapId != null && sid != null && sid in 1..1235) {
                currentWildBattleRecorded = true
                if (encounterMapId !in routeVisitOrder) routeVisitOrder.add(encounterMapId)
                val list = encountersByRoute.getOrPut(encounterMapId) { mutableListOf() }
                if (sid !in list) {
                    list.add(sid)
                    TrackerLog.d(TAG, "new encounter sid=$sid mapId=$encounterMapId gameCode=$gameCode — saving")
                    // Persist to RunData so encounters survive app restarts
                    env?.let { ctx ->
                        val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                        val routeList = data.routeEncounters.getOrPut(encounterMapId.toString()) { mutableListOf() }
                        if (sid !in routeList) routeList.add(sid)
                        TrackerLog.d(TAG, "saving routeEncounters=${data.routeEncounters}")
                        RunRepository.save(ctx.fileStore, romFamilyKey, data)
                    }
                } else {
                    TrackerLog.d(TAG, "encounter sid=$sid mapId=$encounterMapId already in list=$list — skipping")
                }
            }
        }

        // ── Game over detection ───────────────────────────────────────────────
        // Mirrors Lua GameOverScreen.checkForGameOver — three selectable conditions.
        // Exclude catching tutorial battle — mirrors Lua Battle.recentBattleWasTutorial guard.
        if (wasBattleActive && !battleRaw.isActive && !isGameOver.value && !recentBattleWasTutorial) {
            val condition = env?.settings?.getGameOverCondition() ?: GameOverCondition.LEAD_FAINTS
            val isLost = when (condition) {
                GameOverCondition.LEAD_FAINTS -> {
                    val lead = party.firstOrNull()
                    lead != null && !lead.isAlive
                }
                GameOverCondition.HIGHEST_LEVEL_FAINTS -> {
                    val maxLevel = party.filter { it.level > 0 }.maxOfOrNull { it.level } ?: 0
                    maxLevel > 0 && party.any { it.level == maxLevel && !it.isAlive }
                }
                GameOverCondition.ENTIRE_PARTY_FAINTS -> {
                    val realPokemon = party.filter { it.level > 0 }
                    realPokemon.isNotEmpty() && realPokemon.all { !it.isAlive }
                }
            }
            if (isLost && isGameOver.compareAndSet(false, true)) {
                runAttempts++
                env?.let { ctx ->
                    val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                    data.stats.attempts = runAttempts
                    RunRepository.save(ctx.fileStore, romFamilyKey, data)
                }
            }
        }
        // Auto-reset if party count drops to 0 (new game started / ROM reset)
        if (count == 0) {
            isGameOver.value = false
            revealedBySpecies.clear()
            battleRevealedByKey.clear()
            battleFourConfirmedKey = null
            enemyStartingMoveset = emptySet()
            lastEnemyMoveId = 0
            lastKnownEnemySpeciesId = 0
            battleRevealedByKey2.clear()
            battleFourConfirmedKey2 = null
            enemy2StartingMoveset = emptySet()
            lastKnownEnemy2SpeciesId = 0
            // Do NOT clear encountersByRoute or visitedRoutes here — party is transiently empty at
            // ROM load, which would wipe restored data. resetGameOver() handles clearing on new run.
            currentWildBattleRecorded = false
        }

        // ── Trainer defeat event tracking (MaxFR / NatDex ROM hacks) ────────────
        // For ROM hacks, trainer IDs in our table may differ from the ROM's actual IDs,
        // making flag-based reads unreliable. Instead, count defeats as they happen.
        // Fires when a trainer battle ends cleanly (not tutorial, not wild).
        if ((maxFrVariant != MaxFrVariant.NONE || isNatDex)
                && wasBattleActive && !battleRaw.isActive
                && !lastBattleWasWild && !recentBattleWasTutorial) {
            route?.mapLayoutId?.let { mapId ->
                // A two-trainer double battle ends once but defeats BOTH trainers.
                val defeatedNow = if (lastBattleWasTwoTrainerDouble) 2 else 1
                trainerDefeatsByRoute[mapId] = (trainerDefeatsByRoute[mapId] ?: 0) + defeatedNow
                env?.let { ctx ->
                    val data = RunRepository.load(ctx.fileStore, romFamilyKey)
                    data.trainerDefeatsByRoute[mapId.toString()] = trainerDefeatsByRoute[mapId]!!
                    RunRepository.save(ctx.fileStore, romFamilyKey, data)
                }
            }
        }

        // ── Game stats (steps / battles / center visits) ─────────────────────
        // MAX_FR_GEN4's SaveBlock1 game-stats offset could not be resolved (the
        // block reads all-zero / garbage at every reasoned offset), so hide stats
        // for that variant rather than show wrong numbers. Everything else on gen4
        // (battle, route) uses the same SaveBlock1 pointer and works.
        val stats = if (maxFrVariant == MaxFrVariant.MAX_FR_GEN4) null
                    else StatsReader.read(addresses)

        // ── Trainer defeat counts ─────────────────────────────────────────────
        // ROM hacks (MaxFR / NatDex): use session-tracked event counts (ROM-agnostic).
        // Vanilla FRLG/Emerald: read live from SaveBlock1 flag bits.
        val trainerTable = TrainerRouteTable.get(game)
        val singleFightMaps = TrainerRouteTable.getSingleFightMaps(game)
        val rivalIds = TrainerRouteTable.getRivalIds(game)
        val rawTrainerCounts: Map<Int, Pair<Int, Int>> = if (maxFrVariant != MaxFrVariant.NONE || isNatDex) {
            buildMap {
                for ((mapId, trainerIds) in trainerTable) {
                    val defeats = trainerDefeatsByRoute[mapId] ?: 0
                    if (mapId in singleFightMaps) {
                        put(mapId, minOf(defeats, 1) to 1)
                    } else {
                        put(mapId, defeats to TrainerRouteTable.defeatableTotal(trainerIds, rivalIds))
                    }
                }
                // Include any routes with session defeats not in the table
                for ((mapId, defeats) in trainerDefeatsByRoute) {
                    if (mapId !in this) put(mapId, defeats to defeats)
                }
            }
        } else {
            TrainerFlagReader.readCounts(addresses, trainerTable, singleFightMaps, rivalIds)
        }
        // Collapse combined dungeon/gym areas so every floor shows the same aggregate count
        // (matches the Lua tracker). Totals come from the trainer table (authoritative) so
        // session-path fallback entries can't double-count. No-op for non-combined maps.
        val tableTotals = trainerTable.mapValues {
            TrainerRouteTable.defeatableTotal(it.value, rivalIds)
        }
        val trainerCounts = CombinedAreas.collapseCounts(game, rawTrainerCounts, tableTotals)

        // ── Healing items (matches Lua Program.updateBagItems + recalcLeadPokemonHealingInfo) ──
        // Healing % is relative to the active mon (activeIdx = party lead out of battle).
        val bagDetail = party.getOrNull(activeIdx)?.let { active ->
            BagReader.read(addresses, active.maxHp)
        }

        // ── Learnsets (Lua PokemonData.readLevelUpMoves + Utils.getMovesLearnedHeader) ─────
        val playerLearnset = party.getOrNull(activeIdx)?.let { active ->
            LearnsetReader.read(active.speciesId, active.level, addresses)
        }
        val enemyLearnset = battleRaw.enemy?.let { enemy ->
            LearnsetReader.read(enemy.speciesId, enemy.level, addresses)
        }

        // ── Move staleness (Lua Utils.calculateMoveStars) — patch onto enemy after learnset read ──
        // Only applies to the persistent-list display (not when all 4 confirmed this battle,
        // since those moves were just used and are definitely current).
        val rawEnemy = battleRaw.enemy
        val battle = if (rawEnemy != null && enemyLearnset != null
                        && rawEnemy.fourConfirmedThisBattle == null) {
            val displayMoves = revealedBySpecies[rawEnemy.speciesId]?.take(4) ?: emptyList()
            val staleFlags = calculateMoveStaleFlags(
                displayMoves, enemyLearnset.allMoveLevels, rawEnemy.level)
            battleRaw.copy(enemy = rawEnemy.copy(moveStaleFlags = staleFlags))
        } else battleRaw

        return TrackerState.Active(
            game = game, romVersion = romVersion, romTitle = romTitle,
            party = liveParty, battle = battle, currentRoute = route,
            stats = stats, bagDetail = bagDetail,
            isGameOver = isGameOver.value, runAttempts = runAttempts,
            logAvailable = logAvailable.value,
            playerLearnset = playerLearnset, enemyLearnset = enemyLearnset,
            routeEncounters = encountersByRoute.mapValues { it.value.toList() },
            routeVisitOrder = routeVisitOrder.toList(),
            trainerCounts = trainerCounts,
            visitedRoutes = visitedRoutes.toSet(),
            showBallPicker = showBallPicker,
            chosenBall = chosenBall.value,
            isNatDex = isNatDex,
            isMaxFr = maxFrVariant != MaxFrVariant.NONE,
            moveCategories = moveCategories,
            speciesName = { id -> addresses.extPokemonMap[id]?.name ?: SpeciesNames.get(id) },
            natDexId = { id -> addresses.extPokemonMap[id]?.spriteId ?: id },
            pokemonNotes = pokemonNotesCache.toMap(),
        )
    }

    private fun pollBattle(game: GameVersion, addresses: GameAddresses): BattleState {
        val battlersCount = MemoryBridge.readU8(addresses.battlersCount) ?: return BattleState.NONE
        // gBattleOutcome: 0 = battle ongoing, non-zero = battle ended
        val battleOutcome = MemoryBridge.readU8(addresses.battleOutcome) ?: return BattleState.NONE
        val isActive = battlersCount > 0 && battleOutcome == 0

        // Battle type flags: BATTLE_TYPE_TRAINER = 0x08, BATTLE_TYPE_DOUBLE = 0x01
        val typeFlags = MemoryBridge.readBytes(addresses.battleTypeFlags, 4)
        val isWild = typeFlags != null && (typeFlags[0].toInt() and DataHelper.BATTLE_TYPE_TRAINER) == 0

        // gBattlersCount == 4 means 2v2 doubles (Lua: Battle.numBattlers == 4), but on some
        // ROM hacks (Max variants) gBattlersCount reads a garbage value ≥ 4 in single battles.
        // Also require the engine's own BATTLE_TYPE_DOUBLE flag, which is 0 in a real single battle.
        val isDoublesFlag = typeFlags != null && (typeFlags[0].toInt() and DataHelper.BATTLE_TYPE_DOUBLE) != 0
        val isDoubles = isActive && isDoublesFlag && battlersCount >= 4
        // Two SEPARATE opposing trainers (Emerald optional double battle) — bit 15 lives in
        // byte 1 (0x8000 >> 8 = 0x80). Winning ends one battle but defeats TWO trainers.
        val isTwoTrainerDouble = isDoubles && !isWild && typeFlags != null &&
            (typeFlags[1].toInt() and (DataHelper.BATTLE_TYPE_TWO_OPPONENTS shr 8)) != 0

        // Detect battle transitions
        if (!isActive && lastBattleActive) {
            lastEnemyMoveId = 0
            battleRevealedByKey.clear()
            battleFourConfirmedKey = null
            battleRevealedByKey2.clear()
            battleFourConfirmedKey2 = null
        }
        if (isActive && !lastBattleActive) battleJustStarted = true
        lastBattleActive = isActive

        if (!isActive) return BattleState.NONE

        // ── Pre-read enemy2 slot data (doubles only) ────────────────────────────
        // Slot layout: 0=PlayerLeft, 1=EnemyLeft, 2=PlayerRight, 3=EnemyRight
        // offsetBattlePokemonDoublesPartner = 0xB0 = 2×BATTLE_MON_SIZE (Lua Program.lua)
        val enemy2Mon: ByteArray? = if (isDoubles)
            MemoryBridge.readBytes(addresses.battleMons + 3L * DataHelper.BATTLE_MON_SIZE, DataHelper.BATTLE_MON_SIZE)
            else null
        val speciesId2: Int = enemy2Mon?.u16(DataHelper.BMON_SPECIES)?.let { if (it in 1..1235) it else 0 } ?: 0
        // gBattlerPartyIndexes: +6 = RightOther (Lua: gBattlerPartyIndexes + 6)
        val activeEnemy2Slot: Int = if (isDoubles && speciesId2 > 0 && addresses.gBattlerPartyIndexes != 0L) {
            (MemoryBridge.readU8(addresses.gBattlerPartyIndexes + 6L) ?: 0).coerceIn(0, 5)
        } else 0
        val enemy2Raw: ByteArray? = if (speciesId2 > 0)
            MemoryBridge.readBytes(
                addresses.enemyParty + activeEnemy2Slot * DataHelper.POKEMON_STRUCT_SIZE,
                DataHelper.POKEMON_STRUCT_SIZE
            ) else null
        val level2: Int = if (enemy2Raw != null) enemy2Raw[DataHelper.OFF_LEVEL].toInt() and 0xFF else 0

        // ── Enemy gBattleMons slot 1 (LeftOther) ────────────────────────────────
        val enemyMonAddr = addresses.battleMons + DataHelper.BATTLE_MON_SIZE
        val enemyMon = MemoryBridge.readBytes(enemyMonAddr, DataHelper.BATTLE_MON_SIZE)

        val enemy: EnemyData? = if (enemyMon != null) {
            val speciesId = enemyMon.u16(DataHelper.BMON_SPECIES)
            if (speciesId in 1..1235) {
                // gBattlerPartyIndexes[2] = LeftOther party slot (Lua: Battle.Combatants.LeftOther)
                val activeEnemySlot: Int = if (addresses.gBattlerPartyIndexes != 0L) {
                    val idx = MemoryBridge.readU8(addresses.gBattlerPartyIndexes + 2L) ?: 0
                    idx.coerceIn(0, 5)
                } else 0
                val enemyRaw = MemoryBridge.readBytes(
                    addresses.enemyParty + activeEnemySlot * DataHelper.POKEMON_STRUCT_SIZE,
                    DataHelper.POKEMON_STRUCT_SIZE
                )

                val level = if (enemyRaw != null) enemyRaw[DataHelper.OFF_LEVEL].toInt() and 0xFF else 0

                // ── Move revelation via gBattleResults ─────────────────────────────
                // In doubles, gBattleResults.enemyUsedMove reflects the last move from EITHER enemy.
                // We validate against each enemy's starting moveset to attribute moves correctly.
                val currentEnemyMoveId = MemoryBridge.readU16(
                    addresses.battleResults + DataHelper.BATTLE_RESULTS_ENEMY_MOVE_OFFSET
                ) ?: 0

                if (battleJustStarted) {
                    // Snapshot enemy1 starting moveset (Lua BattleParties snapshot)
                    val startMoves = mutableSetOf<Int>()
                    for (offset in intArrayOf(DataHelper.BMON_MOVE1, DataHelper.BMON_MOVE2,
                                              DataHelper.BMON_MOVE3, DataHelper.BMON_MOVE4)) {
                        val m = enemyMon.u16(offset)
                        if (m != 0) startMoves.add(m)
                    }
                    enemyStartingMoveset = startMoves
                    lastKnownEnemySpeciesId = speciesId
                    lastEnemyMoveId = 0
                    // Snapshot enemy2 starting moveset (doubles)
                    if (isDoubles && enemy2Mon != null && speciesId2 > 0) {
                        val startMoves2 = mutableSetOf<Int>()
                        for (offset in intArrayOf(DataHelper.BMON_MOVE1, DataHelper.BMON_MOVE2,
                                                  DataHelper.BMON_MOVE3, DataHelper.BMON_MOVE4)) {
                            val m = enemy2Mon.u16(offset)
                            if (m != 0) startMoves2.add(m)
                        }
                        enemy2StartingMoveset = startMoves2
                        lastKnownEnemy2SpeciesId = speciesId2
                    }
                    battleRevealedByKey.clear()
                    battleFourConfirmedKey = null
                    battleRevealedByKey2.clear()
                    battleFourConfirmedKey2 = null
                    battleJustStarted = false
                } else {
                    // Enemy1 species change — trainer sent out a new Pokémon
                    if (speciesId != lastKnownEnemySpeciesId) {
                        val switchMoves = mutableSetOf<Int>()
                        for (offset in intArrayOf(DataHelper.BMON_MOVE1, DataHelper.BMON_MOVE2,
                                                  DataHelper.BMON_MOVE3, DataHelper.BMON_MOVE4)) {
                            val m = enemyMon.u16(offset)
                            if (m != 0) switchMoves.add(m)
                        }
                        enemyStartingMoveset = switchMoves
                        lastKnownEnemySpeciesId = speciesId
                        lastEnemyMoveId = currentEnemyMoveId
                    }
                    // Enemy2 species change (doubles)
                    if (isDoubles && enemy2Mon != null && speciesId2 > 0 && speciesId2 != lastKnownEnemy2SpeciesId) {
                        val switchMoves2 = mutableSetOf<Int>()
                        for (offset in intArrayOf(DataHelper.BMON_MOVE1, DataHelper.BMON_MOVE2,
                                                  DataHelper.BMON_MOVE3, DataHelper.BMON_MOVE4)) {
                            val m = enemy2Mon.u16(offset)
                            if (m != 0) switchMoves2.add(m)
                        }
                        enemy2StartingMoveset = switchMoves2
                        lastKnownEnemy2SpeciesId = speciesId2
                    }
                    // New move detected — attribute to enemy1, enemy2, or both based on starting moveset
                    if (currentEnemyMoveId != 0 && currentEnemyMoveId != lastEnemyMoveId) {
                        // Skip if HITMARKER_UNABLE_TO_USE_MOVE is set (Truant, full paralysis, etc.)
                        // Mirrors Lua Battle.lua hitmarkerFlag80000 check.
                        val hitMarker = if (addresses.gHitMarker != 0L)
                            MemoryBridge.readU32(addresses.gHitMarker) ?: 0L else 0L
                        val moveBlocked = (hitMarker and DataHelper.HITMARKER_UNABLE_TO_USE) != 0L
                        // Skip if enemy already fainted this turn (player KO'd them before they acted).
                        val enemyFainted = enemyRaw != null && level > 0 &&
                            enemyRaw.u16(DataHelper.OFF_CURRENT_HP) == 0
                        // Always advance lastEnemyMoveId so a blocked value doesn't re-trigger next poll.
                        lastEnemyMoveId = currentEnemyMoveId
                        if (!moveBlocked && !enemyFainted) {
                            // Track for enemy1
                            if (currentEnemyMoveId in enemyStartingMoveset || enemyStartingMoveset.isEmpty()) {
                                val persistent = revealedBySpecies.getOrPut(speciesId) { mutableListOf() }
                                val existingIdx = persistent.indexOfFirst { it.id == currentEnemyMoveId }
                                if (existingIdx == -1) {
                                    persistent.add(0, TrackedMove(currentEnemyMoveId, level, level, level))
                                } else {
                                    val entry = persistent[existingIdx]
                                    entry.level = level
                                    if (level < entry.minLv) entry.minLv = level
                                    if (level > entry.maxLv) entry.maxLv = level
                                    if (existingIdx > 3) { persistent.removeAt(existingIdx); persistent.add(0, entry) }
                                }
                                val battleKey = speciesId * 1000L + level
                                if (battleFourConfirmedKey != battleKey) {
                                    val battleRevealed = battleRevealedByKey.getOrPut(battleKey) { mutableListOf() }
                                    if (currentEnemyMoveId !in battleRevealed) {
                                        battleRevealed.add(currentEnemyMoveId)
                                        if (battleRevealed.size == 4) battleFourConfirmedKey = battleKey
                                    }
                                }
                            }
                            // Track for enemy2 (doubles)
                            if (isDoubles && speciesId2 > 0 &&
                                (currentEnemyMoveId in enemy2StartingMoveset || enemy2StartingMoveset.isEmpty())) {
                                val persistent2 = revealedBySpecies.getOrPut(speciesId2) { mutableListOf() }
                                val existingIdx2 = persistent2.indexOfFirst { it.id == currentEnemyMoveId }
                                if (existingIdx2 == -1) {
                                    persistent2.add(0, TrackedMove(currentEnemyMoveId, level2, level2, level2))
                                } else {
                                    val entry2 = persistent2[existingIdx2]
                                    entry2.level = level2
                                    if (level2 < entry2.minLv) entry2.minLv = level2
                                    if (level2 > entry2.maxLv) entry2.maxLv = level2
                                    if (existingIdx2 > 3) { persistent2.removeAt(existingIdx2); persistent2.add(0, entry2) }
                                }
                                val battleKey2 = speciesId2 * 1000L + level2
                                if (battleFourConfirmedKey2 != battleKey2) {
                                    val battleRevealed2 = battleRevealedByKey2.getOrPut(battleKey2) { mutableListOf() }
                                    if (currentEnemyMoveId !in battleRevealed2) {
                                        battleRevealed2.add(currentEnemyMoveId)
                                        if (battleRevealed2.size == 4) battleFourConfirmedKey2 = battleKey2
                                    }
                                }
                            }
                        }
                    }
                }

                var ability1Id = 0; var ability2Id = 0
                val baseStats = MemoryBridge.readBytes(
                    addresses.baseStatsTable + speciesId * DataHelper.BASE_STATS_ENTRY_SIZE,
                    DataHelper.BASE_STATS_ENTRY_SIZE
                )
                if (baseStats != null) {
                    ability1Id = baseStats[DataHelper.BASE_STATS_ABILITY1].toInt() and 0xFF
                    ability2Id = baseStats[DataHelper.BASE_STATS_ABILITY2].toInt() and 0xFF
                }

                val type1 = (enemyMon[DataHelper.BMON_TYPE1].toInt() and 0xFF)
                    .let { if (it <= 18) it else baseStats?.get(DataHelper.BASE_STATS_TYPE1)?.toInt()?.and(0xFF) ?: 0 }
                val type2 = (enemyMon[DataHelper.BMON_TYPE2].toInt() and 0xFF)
                    .let { if (it <= 18) it else baseStats?.get(DataHelper.BASE_STATS_TYPE2)?.toInt()?.and(0xFF) ?: 0 }
                val bst = addresses.extPokemonMap[speciesId]?.bst ?: BstTable.bst(speciesId)

                val currentHp = if (enemyRaw != null) enemyRaw.u16(DataHelper.OFF_CURRENT_HP) else 0
                val maxHpRaw = if (enemyRaw != null) enemyRaw.u16(DataHelper.OFF_MAX_HP) else 0

                val enemyPpByMoveId: Map<Int, Int> = if (enemyRaw != null && enemyRaw.size >= 100) {
                    val ePers = enemyRaw.u32(DataHelper.OFF_PERSONALITY)
                    val eOt   = enemyRaw.u32(DataHelper.OFF_OT_ID)
                    val eKey  = ePers xor eOt
                    val eDec  = ByteArray(48)
                    for (wi in 0 until 12) {
                        val w = enemyRaw.u32(DataHelper.OFF_ENCRYPTED + wi * 4) xor eKey
                        eDec[wi*4+0] = (w and 0xFF).toByte()
                        eDec[wi*4+1] = ((w shr 8) and 0xFF).toByte()
                        eDec[wi*4+2] = ((w shr 16) and 0xFF).toByte()
                        eDec[wi*4+3] = ((w shr 24) and 0xFF).toByte()
                    }
                    val eOrder = PokemonDecoder.SUBSTRUCTURE_ORDER[(ePers % 24).toInt()]
                    val atkOff = eOrder[1] * 12
                    buildMap {
                        for (slot in 0..3) {
                            val mId = (eDec[atkOff + slot*2].toInt() and 0xFF) or
                                      ((eDec[atkOff + slot*2 + 1].toInt() and 0xFF) shl 8)
                            val pp  = eDec[atkOff + 8 + slot].toInt() and 0xFF
                            if (mId != 0) put(mId, pp)
                        }
                    }
                } else emptyMap()

                val persistent = revealedBySpecies[speciesId]
                val battleKey = speciesId * 1000L + level
                val fourConfirmed = if (battleFourConfirmedKey == battleKey)
                    battleRevealedByKey[battleKey]?.toList() else null
                val displayIds = fourConfirmed ?: persistent?.take(4)?.map { it.id } ?: emptyList()

                EnemyData(
                    speciesId               = speciesId,
                    natDexId                = addresses.extPokemonMap[speciesId]?.spriteId ?: speciesId,
                    name                    = addresses.extPokemonMap[speciesId]?.name ?: SpeciesNames.get(speciesId),
                    level                   = level,
                    type1                   = type1,
                    type2                   = type2,
                    ability1Id              = ability1Id,
                    ability2Id              = ability2Id,
                    bst                     = bst,
                    revealedMoveIds         = displayIds,
                    ppByMoveId              = enemyPpByMoveId,
                    status                  = enemyMon[DataHelper.BMON_STATUS].toInt() and 0xFF,
                    currentHp               = currentHp,
                    maxHp                   = maxHpRaw,
                    totalTrackedMoveCount   = persistent?.size ?: 0,
                    fourConfirmedThisBattle = fourConfirmed,
                    allTrackedMoves         = persistent?.toList() ?: emptyList(),
                    statStages              = statStagesFrom(enemyMon),
                    // moveStaleFlags populated in poll() after learnset read
                )
            } else null
        } else null

        // ── Enemy 2 decode (doubles, slot 3 = RightOther) ────────────────────────
        val enemy2: EnemyData? = if (isDoubles && enemy2Mon != null && speciesId2 > 0) {
            var ability1Id2 = 0; var ability2Id2 = 0
            val baseStats2 = MemoryBridge.readBytes(
                addresses.baseStatsTable + speciesId2 * DataHelper.BASE_STATS_ENTRY_SIZE,
                DataHelper.BASE_STATS_ENTRY_SIZE
            )
            if (baseStats2 != null) {
                ability1Id2 = baseStats2[DataHelper.BASE_STATS_ABILITY1].toInt() and 0xFF
                ability2Id2 = baseStats2[DataHelper.BASE_STATS_ABILITY2].toInt() and 0xFF
            }
            val type1_2 = (enemy2Mon[DataHelper.BMON_TYPE1].toInt() and 0xFF)
                .let { if (it <= 18) it else baseStats2?.get(DataHelper.BASE_STATS_TYPE1)?.toInt()?.and(0xFF) ?: 0 }
            val type2_2 = (enemy2Mon[DataHelper.BMON_TYPE2].toInt() and 0xFF)
                .let { if (it <= 18) it else baseStats2?.get(DataHelper.BASE_STATS_TYPE2)?.toInt()?.and(0xFF) ?: 0 }
            val bst2 = addresses.extPokemonMap[speciesId2]?.bst ?: BstTable.bst(speciesId2)
            val currentHp2 = if (enemy2Raw != null) enemy2Raw.u16(DataHelper.OFF_CURRENT_HP) else 0
            val maxHpRaw2  = if (enemy2Raw != null) enemy2Raw.u16(DataHelper.OFF_MAX_HP) else 0
            val enemyPpByMoveId2: Map<Int, Int> = if (enemy2Raw != null && enemy2Raw.size >= 100) {
                val ePers = enemy2Raw.u32(DataHelper.OFF_PERSONALITY)
                val eOt   = enemy2Raw.u32(DataHelper.OFF_OT_ID)
                val eKey  = ePers xor eOt
                val eDec  = ByteArray(48)
                for (wi in 0 until 12) {
                    val w = enemy2Raw.u32(DataHelper.OFF_ENCRYPTED + wi * 4) xor eKey
                    eDec[wi*4+0] = (w and 0xFF).toByte()
                    eDec[wi*4+1] = ((w shr 8) and 0xFF).toByte()
                    eDec[wi*4+2] = ((w shr 16) and 0xFF).toByte()
                    eDec[wi*4+3] = ((w shr 24) and 0xFF).toByte()
                }
                val eOrder2 = PokemonDecoder.SUBSTRUCTURE_ORDER[(ePers % 24).toInt()]
                val atkOff2 = eOrder2[1] * 12
                buildMap {
                    for (slot in 0..3) {
                        val mId = (eDec[atkOff2 + slot*2].toInt() and 0xFF) or
                                  ((eDec[atkOff2 + slot*2 + 1].toInt() and 0xFF) shl 8)
                        val pp  = eDec[atkOff2 + 8 + slot].toInt() and 0xFF
                        if (mId != 0) put(mId, pp)
                    }
                }
            } else emptyMap()
            val persistent2 = revealedBySpecies[speciesId2]
            val battleKey2 = speciesId2 * 1000L + level2
            val fourConfirmed2 = if (battleFourConfirmedKey2 == battleKey2)
                battleRevealedByKey2[battleKey2]?.toList() else null
            val displayIds2 = fourConfirmed2 ?: persistent2?.take(4)?.map { it.id } ?: emptyList()
            EnemyData(
                speciesId               = speciesId2,
                natDexId                = addresses.extPokemonMap[speciesId2]?.spriteId ?: speciesId2,
                name                    = addresses.extPokemonMap[speciesId2]?.name ?: SpeciesNames.get(speciesId2),
                level                   = level2,
                type1                   = type1_2,
                type2                   = type2_2,
                ability1Id              = ability1Id2,
                ability2Id              = ability2Id2,
                bst                     = bst2,
                revealedMoveIds         = displayIds2,
                ppByMoveId              = enemyPpByMoveId2,
                status                  = enemy2Mon[DataHelper.BMON_STATUS].toInt() and 0xFF,
                currentHp               = currentHp2,
                maxHp                   = maxHpRaw2,
                totalTrackedMoveCount   = persistent2?.size ?: 0,
                fourConfirmedThisBattle = fourConfirmed2,
                allTrackedMoves         = persistent2?.toList() ?: emptyList(),
                statStages              = statStagesFrom(enemy2Mon),
            )
        } else null

        // ── Player live types (gBattleMons slot 0) ──────────────────────────────
        val playerTypeBytes = MemoryBridge.readBytes(
            addresses.battleMons + DataHelper.BMON_TYPE1.toLong(), 2
        )
        val playerType1 = playerTypeBytes?.get(0)?.toInt()?.and(0xFF) ?: -1
        val playerType2 = playerTypeBytes?.get(1)?.toInt()?.and(0xFF) ?: -1

        // ── Player stat stages (gBattleMons slot 0, offset 0x18) ────────────────
        // Memory layout: [HP, Atk, Def, Spe, SpA, SpD, Acc, Eva]; display: [Atk,Def,SpA,SpD,Spe,Acc,Eva]
        val playerStatStages = MemoryBridge.readBytes(addresses.battleMons + 0x18L, 8)
            ?.let { b ->
                intArrayOf(
                    b[1].toInt() and 0xFF,  // Atk
                    b[2].toInt() and 0xFF,  // Def
                    b[4].toInt() and 0xFF,  // SpA
                    b[5].toInt() and 0xFF,  // SpD
                    b[3].toInt() and 0xFF,  // Spe
                    b[6].toInt() and 0xFF,  // Acc
                    b[7].toInt() and 0xFF,  // Eva
                )
            }

        // ── Active player party indices (gBattlerPartyIndexes) ─────────────────
        // +0 = LeftOwn, +4 = RightOwn (Lua: gBattlerPartyIndexes + 4).
        // LeftOwn is read in singles too: after a mid-battle switch the on-field mon is
        // often NOT party slot 0, so this drives which mon the tracker displays. Mirrors
        // the enemy read above and Lua Battle.updateViewSlots (re-read every tick).
        val playerMon1PartyIdx = if (addresses.gBattlerPartyIndexes != 0L) {
            (MemoryBridge.readU8(addresses.gBattlerPartyIndexes + 0L) ?: 0).coerceIn(0, 5)
        } else 0
        val playerMon2PartyIdx = if (isDoubles && addresses.gBattlerPartyIndexes != 0L) {
            (MemoryBridge.readU8(addresses.gBattlerPartyIndexes + 4L) ?: 0).coerceIn(0, 5)
        } else -1

        // ── Trainer opponent ID ──────────────────────────────────────────────────
        val trainerOpponentId = if (!isWild && addresses.trainerBattleOpponent != 0L)
            MemoryBridge.readU16(addresses.trainerBattleOpponent) ?: 0
        else 0

        // ── Weather ──────────────────────────────────────────────────────────────
        val weatherBytes = MemoryBridge.readBytes(addresses.battleWeather, 2)
        val weatherBits = weatherBytes?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        } ?: 0
        val weather = when {
            weatherBits and 0x07 != 0 -> Weather.RAIN
            weatherBits and 0x18 != 0 -> Weather.SAND
            weatherBits and 0x60 != 0 -> Weather.SUN
            weatherBits and 0x80 != 0 -> Weather.HAIL
            else -> Weather.NONE
        }

        // ── Side statuses ────────────────────────────────────────────────────────
        val sideStatus = MemoryBridge.readBytes(addresses.sideStatuses, 4)
        val playerSideStatus = sideStatus?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        } ?: 0
        val sideTimer = MemoryBridge.readBytes(addresses.sideTimers, 16)
        val reflectTurns      = sideTimer?.get(4)?.toInt()?.and(0xFF) ?: 0
        val lightScreenTurns  = sideTimer?.get(5)?.toInt()?.and(0xFF) ?: 0
        val safeguardTurns    = sideTimer?.get(6)?.toInt()?.and(0xFF) ?: 0
        val spikes = (playerSideStatus ushr 9) and 0x03

        return BattleState(
            isActive           = true,
            isWild             = isWild,
            enemy              = enemy,
            weather            = weather,
            playerReflect      = reflectTurns,
            playerLightScreen  = lightScreenTurns,
            enemySpikes        = spikes,
            playerSafeguard    = safeguardTurns,
            turnCount          = 0,
            lastMoveId         = 0,
            trainerOpponentId  = trainerOpponentId,
            playerStatStages   = playerStatStages,
            playerType1        = playerType1,
            playerType2        = playerType2,
            isDoubles          = isDoubles,
            isTwoTrainerDouble = isTwoTrainerDouble,
            enemy2             = enemy2,
            playerMon1PartyIdx = playerMon1PartyIdx,
            playerMon2PartyIdx = playerMon2PartyIdx,
        )
    }

    /**
     * Mirrors Lua Utils.calculateMoveStars.
     * For each of the top-4 displayed moves, returns true if the move may have been replaced
     * since it was last seen — i.e., the Pokémon has had enough level-up opportunities to forget it.
     */
    private fun calculateMoveStaleFlags(
        displayMoves: List<TrackedMove>,
        allMoveLevels: List<Int>,
        currentLevel: Int,
    ): List<Boolean> {
        if (displayMoves.isEmpty()) return emptyList()
        val n = displayMoves.size
        // Count how many learnset entries lie between each move's last-seen level and currentLevel
        val movesLearnedSince = IntArray(n) { 0 }
        for (lv in allMoveLevels) {
            for (i in 0 until n) {
                val moveLv = displayMoves[i].level
                if (lv > moveLv && lv <= currentLevel) movesLearnedSince[i]++
            }
        }
        // Rank each move by age relative to the others (older = potentially more forgotten)
        val moveAgeRank = IntArray(n) { 1 }
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i != j && displayMoves[i].level > displayMoves[j].level) moveAgeRank[i]++
            }
        }
        return displayMoves.mapIndexed { i, move ->
            move.level != 1 && movesLearnedSince[i] >= moveAgeRank[i]
        }
    }

    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
        ((this[offset + 1].toLong() and 0xFF) shl 8) or
        ((this[offset + 2].toLong() and 0xFF) shl 16) or
        ((this[offset + 3].toLong() and 0xFF) shl 24)

    // gBattleMons statStages at offset 0x18; reorder memory
    // [HP,Atk,Def,Spe,SpA,SpD,Acc,Eva] -> display [Atk,Def,SpA,SpD,Spe,Acc,Eva]
    private fun statStagesFrom(mon: ByteArray): IntArray = intArrayOf(
        mon[0x19].toInt() and 0xFF, // Atk
        mon[0x1A].toInt() and 0xFF, // Def
        mon[0x1C].toInt() and 0xFF, // SpA
        mon[0x1D].toInt() and 0xFF, // SpD
        mon[0x1B].toInt() and 0xFF, // Spe
        mon[0x1E].toInt() and 0xFF, // Acc
        mon[0x1F].toInt() and 0xFF, // Eva
    )

    private const val POLL_INTERVAL_MS = 250L
    private const val BATTLE_POLL_INTERVAL_MS = 50L  // faster polling during battle for high-speed emulation
    private const val TAG = "TrackerPoller"
}
