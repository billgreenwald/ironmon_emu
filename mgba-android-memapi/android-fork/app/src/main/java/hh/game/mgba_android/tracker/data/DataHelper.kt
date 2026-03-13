package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.GameVersion

data class GameAddresses(
    val partyCount: Long,
    val partyBase: Long,
    val baseStatsTable: Long,
    val levelUpLearnsets: Long,
    val experienceTables: Long,
    // Battle addresses
    val enemyParty: Long,
    val battleTypeFlags: Long,
    val battleMons: Long,
    val battlersCount: Long,
    val battleWeather: Long,
    val sideStatuses: Long,
    val sideTimers: Long,
    val battleOutcome: Long,
    // Map/location
    val gMapHeader: Long,        // read mapLayoutId at gMapHeader + 0x12
    // SaveBlock1 (for stats)
    val saveBlock1Ptr: Long,
    val gameStatsOffset: Int,    // byte offset within SaveBlock1 to game stats array
)

object DataHelper {

    const val POKEMON_STRUCT_SIZE: Int = 100

    // ── Unencrypted header ───────────────────────────────────────────────────
    const val OFF_PERSONALITY: Int = 0x00
    const val OFF_OT_ID:       Int = 0x04
    const val OFF_ENCRYPTED:   Int = 0x20
    const val OFF_LEVEL:       Int = 0x54
    const val OFF_CURRENT_HP:  Int = 0x56
    const val OFF_MAX_HP:      Int = 0x58
    const val OFF_ATTACK:      Int = 0x5A
    const val OFF_DEFENSE:     Int = 0x5C
    const val OFF_SPEED:       Int = 0x5E
    const val OFF_SP_ATK:      Int = 0x60
    const val OFF_SP_DEF:      Int = 0x62

    // ── Growth substructure ──────────────────────────────────────────────────
    const val GROWTH_SPECIES:  Int = 0x00
    const val GROWTH_ITEM:     Int = 0x02
    const val GROWTH_EXP:      Int = 0x04

    // ── Attacks substructure ─────────────────────────────────────────────────
    const val ATK_MOVE1: Int = 0x00
    const val ATK_MOVE2: Int = 0x02
    const val ATK_MOVE3: Int = 0x04
    const val ATK_MOVE4: Int = 0x06
    const val ATK_PP1:   Int = 0x08
    const val ATK_PP2:   Int = 0x09
    const val ATK_PP3:   Int = 0x0A
    const val ATK_PP4:   Int = 0x0B

    // ── Misc substructure ────────────────────────────────────────────────────
    const val MISC_POKERUS:    Int = 0x00
    const val MISC_IV_ABILITY: Int = 0x04

    // ── Base stats ROM (28 bytes/species) ────────────────────────────────────
    const val BASE_STATS_ENTRY_SIZE: Int = 28
    const val BASE_STATS_HP:         Int = 0
    const val BASE_STATS_ATK:        Int = 1
    const val BASE_STATS_DEF:        Int = 2
    const val BASE_STATS_SPD:        Int = 3
    const val BASE_STATS_SP_ATK:     Int = 4
    const val BASE_STATS_SP_DEF:     Int = 5
    const val BASE_STATS_TYPE1:      Int = 6
    const val BASE_STATS_TYPE2:      Int = 7
    const val BASE_STATS_GENDER_RATIO: Int = 16
    const val BASE_STATS_EXP_GROUP:  Int = 19
    const val BASE_STATS_ABILITY1:   Int = 22
    const val BASE_STATS_ABILITY2:   Int = 23

    // ── gBattleMons (struct BattlePokemon = 0x58 bytes) ──────────────────────
    // Layout confirmed from Lua tracker (statStages@0x18, types@0x21):
    //   0x00: species, 0x02-0x0B: combat stats (5×u16)
    //   0x0C-0x13: moves[4] (u16 each)
    //   0x14-0x17: pp[4] (u8 each)
    //   0x18-0x1F: statStages[8] (u8 each)
    //   0x20: flags, 0x21: type1, 0x22: type2
    const val BATTLE_MON_SIZE: Int = 0x58  // 88 bytes per slot
    const val BMON_SPECIES:    Int = 0x00
    const val BMON_MOVE1:      Int = 0x0C
    const val BMON_MOVE2:      Int = 0x0E
    const val BMON_MOVE3:      Int = 0x10
    const val BMON_MOVE4:      Int = 0x12
    const val BMON_STATUS:     Int = 0x28  // status1 (approximate; used for display only)

    // ── Map header offset ────────────────────────────────────────────────────
    const val MAP_HEADER_LAYOUT_ID_OFFSET: Int = 0x12  // u16 mapLayoutId

    // =========================================================================
    // Per-game addresses
    // Version detection: read 1 byte at 0x080000BC (0=v1.0, 1=v1.1, 2=v1.2)
    // =========================================================================

    // FireRed English v1.0 (BPRE, version byte 0)
    private val FIRE_RED_V10 = GameAddresses(
        partyCount        = 0x02024029L,
        partyBase         = 0x02024284L,
        baseStatsTable    = 0x08254784L,
        levelUpLearnsets  = 0x0825D794L,
        experienceTables  = 0x08253AE4L,
        enemyParty        = 0x0202402CL,
        battleTypeFlags   = 0x02022B4CL,
        battleMons        = 0x02023BE4L,
        battlersCount     = 0x02023BCCL,
        battleWeather     = 0x02023F1CL,
        sideStatuses      = 0x02023DDEL,
        sideTimers        = 0x02023DE4L,
        battleOutcome     = 0x02023E8AL,
        gMapHeader        = 0x02036DFCL,
        saveBlock1Ptr     = 0x03005008L,
        gameStatsOffset   = 0x1200,
    )

    // FireRed English v1.1 (BPRE, version byte 1)
    private val FIRE_RED_V11 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x082547F4L,
        levelUpLearnsets = 0x0825D804L,
        experienceTables = 0x08253B54L,
    )

    // FireRed non-English (Japanese uses 0x821118C, others vary)
    // Spanish=0x824FF4C, Italian=0x824D864, French=0x824EBD4, German=0x824EBD4-ish
    // For simplicity, we group by code suffix — added below in addressesFor

    // LeafGreen English v1.0
    private val LEAF_GREEN_V10 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x08254760L,
        levelUpLearnsets = 0x0825D770L,
        experienceTables = 0x08253AC0L,
    )

    // LeafGreen English v1.1
    private val LEAF_GREEN_V11 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x082547D0L,
        levelUpLearnsets = 0x0825D804L,
        experienceTables = 0x08253B30L,
    )

    // Ruby v1.0
    private val RUBY_V10 = GameAddresses(
        partyCount        = 0x03004350L,
        partyBase         = 0x020244ECL,
        baseStatsTable    = 0x081FEC18L,
        levelUpLearnsets  = 0x0823B16CL,
        experienceTables  = 0x081E8CE4L,
        enemyParty        = 0x020244ECL,
        battleTypeFlags   = 0x03004360L,
        battleMons        = 0x03004360L,
        battlersCount     = 0x03004398L,
        battleWeather     = 0x020238C8L,
        sideStatuses      = 0x02023718L,
        sideTimers        = 0x0202371EL,
        battleOutcome     = 0x03004360L,
        gMapHeader        = 0x0202E828L,
        saveBlock1Ptr     = 0x03005D8CL,
        gameStatsOffset   = 0x1540,
    )

    // Ruby v1.1 / v1.2 share same base stats address
    private val RUBY_V11 = RUBY_V10.copy(
        baseStatsTable   = 0x081FEC30L,
    )

    // Sapphire v1.0
    private val SAPPHIRE_V10 = RUBY_V10.copy(
        baseStatsTable   = 0x081FEBA8L,
        experienceTables = 0x081E8CECL,
    )

    // Sapphire v1.1 / v1.2
    private val SAPPHIRE_V11 = RUBY_V10.copy(
        baseStatsTable   = 0x081FEBC0L,
        experienceTables = 0x081E8CECL,
    )

    // Emerald (single version)
    val EMERALD = GameAddresses(
        partyCount        = 0x020244E9L,
        partyBase         = 0x020244ECL,
        baseStatsTable    = 0x083203CCL,
        levelUpLearnsets  = 0x0832677CL,
        experienceTables  = 0x082E82C4L,
        enemyParty        = 0x020244ECL,
        battleTypeFlags   = 0x02022FECL,
        battleMons        = 0x02024084L,
        battlersCount     = 0x02024074L,
        battleWeather     = 0x020243CCL,
        sideStatuses      = 0x0202428EL,
        sideTimers        = 0x02024294L,
        battleOutcome     = 0x020241FCL,
        gMapHeader        = 0x02037318L,
        saveBlock1Ptr     = 0x03005D8CL,
        gameStatsOffset   = 0x159C,
    )

    /**
     * Returns the correct addresses for [game] and [romVersion] (byte from 0x080000BC).
     * [gameCode] is the 4-char game code used to detect non-English variants.
     */
    fun addressesFor(game: GameVersion, romVersion: Int = 0, gameCode: String = ""): GameAddresses? = when (game) {
        GameVersion.FIRE_RED -> when {
            gameCode == "BPRS" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824FF4CL) // Spanish
            gameCode == "BPRI" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824D864L) // Italian
            gameCode == "BPRF" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824EBD4L) // French
            gameCode == "BPRD" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824EBD4L) // German (approx)
            gameCode == "BPRJ" -> FIRE_RED_V10.copy(baseStatsTable = 0x0821118CL) // Japanese
            romVersion >= 1 -> FIRE_RED_V11
            else -> FIRE_RED_V10
        }
        GameVersion.LEAF_GREEN -> when {
            romVersion >= 1 -> LEAF_GREEN_V11
            else -> LEAF_GREEN_V10
        }
        GameVersion.RUBY -> when {
            romVersion >= 1 -> RUBY_V11
            else -> RUBY_V10
        }
        GameVersion.SAPPHIRE -> when {
            romVersion >= 1 -> SAPPHIRE_V11
            else -> SAPPHIRE_V10
        }
        GameVersion.EMERALD    -> EMERALD
        GameVersion.UNKNOWN    -> null
    }
}
