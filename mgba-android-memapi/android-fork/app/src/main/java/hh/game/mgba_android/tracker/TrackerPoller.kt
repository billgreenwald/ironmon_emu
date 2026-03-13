package hh.game.mgba_android.tracker

import hh.game.mgba_android.tracker.data.DataHelper
import hh.game.mgba_android.tracker.data.GameAddresses
import hh.game.mgba_android.tracker.data.GameSettings
import hh.game.mgba_android.tracker.data.PokemonDecoder
import hh.game.mgba_android.tracker.data.RouteReader
import hh.game.mgba_android.tracker.models.BattleState
import hh.game.mgba_android.tracker.models.EnemyData
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.TrackerState
import hh.game.mgba_android.tracker.models.Weather
import hh.game.mgba_android.tracker.tables.AbilityTable
import hh.game.mgba_android.tracker.tables.MoveNames
import hh.game.mgba_android.tracker.tables.SpeciesNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TrackerPoller {

    private val _state = MutableStateFlow<TrackerState>(TrackerState.Disconnected)
    val state: StateFlow<TrackerState> = _state

    // Track revealed enemy moves across ticks (reset on battle end)
    private var revealedEnemyMoves = mutableSetOf<Int>()
    private var lastBattleActive = false

    private var pollJob: Job? = null

    fun start(scope: CoroutineScope) {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                _state.value = poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = TrackerState.Disconnected
    }

    private fun poll(): TrackerState {
        if (MemoryBridge.reader == null) return TrackerState.Disconnected

        val codeBytes = MemoryBridge.readBytes(GameSettings.ROM_GAME_CODE_ADDR, 4)
            ?: return TrackerState.Disconnected

        val game = GameSettings.detectGame(codeBytes)
        if (game == GameVersion.UNKNOWN) return TrackerState.NoGameLoaded

        // Read ROM version byte (0=v1.0, 1=v1.1, 2=v1.2) and 4-char game code string
        val romVersion = GameSettings.readVersionByte { addr, len -> MemoryBridge.readBytes(addr, len) }
        val gameCode = String(codeBytes.take(4).toByteArray(), Charsets.ISO_8859_1)

        val addresses = DataHelper.addressesFor(game, romVersion, gameCode)
            ?: return TrackerState.NoGameLoaded

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
                    nameTable = { id -> SpeciesNames.get(id) },
                    moveTable = { id -> MoveNames.get(id) },
                    baseStatsReader = { speciesId ->
                        val addr = addresses.baseStatsTable +
                            speciesId * DataHelper.BASE_STATS_ENTRY_SIZE
                        MemoryBridge.readBytes(addr, DataHelper.BASE_STATS_ENTRY_SIZE)
                    },
                )
                if (pokemon != null) add(pokemon)
            }
        }

        // ── Battle ────────────────────────────────────────────────────────────
        val battle = pollBattle(game, addresses)

        // ── Route ─────────────────────────────────────────────────────────────
        val route = RouteReader.read(game, addresses)

        return TrackerState.Active(game = game, romVersion = romVersion, party = party, battle = battle, currentRoute = route)
    }

    private fun pollBattle(game: GameVersion, addresses: GameAddresses): BattleState {
        val battlersCount = MemoryBridge.readU8(addresses.battlersCount) ?: return BattleState.NONE
        // gBattleOutcome: 0 = battle ongoing, non-zero = battle ended
        val battleOutcome = MemoryBridge.readU8(addresses.battleOutcome) ?: return BattleState.NONE
        val isActive = battlersCount > 0 && battleOutcome == 0

        // Reset revealed moves on battle end
        if (!isActive && lastBattleActive) revealedEnemyMoves.clear()
        lastBattleActive = isActive

        if (!isActive) return BattleState.NONE

        // Battle type flags: bit 0 = wild battle
        val typeFlags = MemoryBridge.readBytes(addresses.battleTypeFlags, 4)
        val isWild = typeFlags != null && (typeFlags[0].toInt() and 0x01) != 0

        // ── Enemy gBattleMons slot (slot index 1) ─────────────────────────────
        val enemyMonAddr = addresses.battleMons + DataHelper.BATTLE_MON_SIZE // slot 1
        val enemyMon = MemoryBridge.readBytes(enemyMonAddr, DataHelper.BATTLE_MON_SIZE)

        val enemy: EnemyData? = if (enemyMon != null) {
            val speciesId = enemyMon.u16(DataHelper.BMON_SPECIES)
            if (speciesId in 1..386) {
                // Read enemy move reveals
                for (i in 0..3) {
                    val mId = enemyMon.u16(DataHelper.BMON_MOVE1 + i * 2)
                    // We reveal moves from the enemy's party struct (fully known in the battle engine)
                    // For "as-revealed" tracking, only add when we read the actual battle mon
                    if (mId != 0) revealedEnemyMoves.add(mId)
                }

                // Enemy party struct — for level and HP (party offsets are reliable)
                val enemyRaw = MemoryBridge.readBytes(addresses.enemyParty, DataHelper.POKEMON_STRUCT_SIZE)

                var baseHp = 0; var baseAtk = 0; var baseDef = 0
                var baseSpd = 0; var baseSpAtk = 0; var baseSpDef = 0
                var type1 = 0; var type2 = 0
                var ability1Id = 0; var ability2Id = 0

                val baseStats = MemoryBridge.readBytes(
                    addresses.baseStatsTable + speciesId * DataHelper.BASE_STATS_ENTRY_SIZE,
                    DataHelper.BASE_STATS_ENTRY_SIZE
                )
                if (baseStats != null) {
                    baseHp    = baseStats[DataHelper.BASE_STATS_HP].toInt() and 0xFF
                    baseAtk   = baseStats[DataHelper.BASE_STATS_ATK].toInt() and 0xFF
                    baseDef   = baseStats[DataHelper.BASE_STATS_DEF].toInt() and 0xFF
                    baseSpd   = baseStats[DataHelper.BASE_STATS_SPD].toInt() and 0xFF
                    baseSpAtk = baseStats[DataHelper.BASE_STATS_SP_ATK].toInt() and 0xFF
                    baseSpDef = baseStats[DataHelper.BASE_STATS_SP_DEF].toInt() and 0xFF
                    type1     = baseStats[DataHelper.BASE_STATS_TYPE1].toInt() and 0xFF
                    type2     = baseStats[DataHelper.BASE_STATS_TYPE2].toInt() and 0xFF
                    ability1Id = baseStats[DataHelper.BASE_STATS_ABILITY1].toInt() and 0xFF
                    ability2Id = baseStats[DataHelper.BASE_STATS_ABILITY2].toInt() and 0xFF
                }

                // Level and HP from party struct (reliable offsets)
                val level = if (enemyRaw != null) enemyRaw[DataHelper.OFF_LEVEL].toInt() and 0xFF else 0
                val currentHp = if (enemyRaw != null) enemyRaw.u16(DataHelper.OFF_CURRENT_HP) else 0
                val maxHpRaw = if (enemyRaw != null) enemyRaw.u16(DataHelper.OFF_MAX_HP) else 0

                EnemyData(
                    speciesId       = speciesId,
                    name            = SpeciesNames.get(speciesId),
                    level           = level,
                    type1           = type1,
                    type2           = type2,
                    ability1Id      = ability1Id,
                    ability2Id      = ability2Id,
                    baseHp          = baseHp,
                    baseAtk         = baseAtk,
                    baseDef         = baseDef,
                    baseSpd         = baseSpd,
                    baseSpAtk       = baseSpAtk,
                    baseSpDef       = baseSpDef,
                    revealedMoveIds = revealedEnemyMoves.toList(),
                    status          = enemyMon[DataHelper.BMON_STATUS].toInt() and 0xFF,
                    currentHp       = currentHp,
                    maxHp           = maxHpRaw,
                )
            } else null
        } else null

        // ── Weather ───────────────────────────────────────────────────────────
        val weatherBytes = MemoryBridge.readBytes(addresses.battleWeather, 2)
        val weatherBits = weatherBytes?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        } ?: 0

        val weather = when {
            weatherBits and 0x07 != 0 -> Weather.RAIN   // bits 0-2
            weatherBits and 0x18 != 0 -> Weather.SAND   // bits 3-4
            weatherBits and 0x60 != 0 -> Weather.SUN    // bits 5-6
            weatherBits and 0x80 != 0 -> Weather.HAIL   // bit 7
            else -> Weather.NONE
        }

        // ── Side statuses (reflect/light screen/spikes/safeguard) ────────────
        val sideStatus = MemoryBridge.readBytes(addresses.sideStatuses, 4)
        val playerSideStatus = sideStatus?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        } ?: 0

        // Side timers: 0x00..0x0F = player side timer block
        val sideTimer = MemoryBridge.readBytes(addresses.sideTimers, 16)
        val reflectTurns      = sideTimer?.get(4)?.toInt()?.and(0xFF) ?: 0
        val lightScreenTurns  = sideTimer?.get(5)?.toInt()?.and(0xFF) ?: 0
        val safeguardTurns    = sideTimer?.get(6)?.toInt()?.and(0xFF) ?: 0

        val spikes = (playerSideStatus ushr 9) and 0x03  // bits 9-10

        return BattleState(
            isActive         = true,
            isWild           = isWild,
            enemy            = enemy,
            weather          = weather,
            playerReflect    = reflectTurns,
            playerLightScreen = lightScreenTurns,
            enemySpikes      = spikes,
            playerSafeguard  = safeguardTurns,
            turnCount        = 0,
            lastMoveId       = 0,
        )
    }

    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private const val POLL_INTERVAL_MS = 250L
}
