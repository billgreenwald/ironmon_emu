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
    val battleResults: Long,       // gBattleResults struct (IWRAM)
    // Map/location
    val gMapHeader: Long,        // read mapLayoutId at gMapHeader + 0x12
    // SaveBlock1 (for stats)
    val saveBlock1Ptr: Long,
    val saveBlock1IsPointer: Boolean = true,  // true = dereference pointer; false = use address directly (Ruby/Sapphire)
    val gameStatsOffset: Int,    // byte offset within SaveBlock1 to game stats array
    val gameFlagsOffset: Int,    // byte offset within SaveBlock1 to game flags array (trainer defeat bits)
    // SaveBlock2 (for XOR key used to decrypt game stats)
    // saveBlock2Ptr == 0L means no encryption (Ruby/Sapphire per Lua tracker)
    val saveBlock2Ptr: Long,
    val encryptionKeyOffset: Int, // offset within SaveBlock2 for the 32-bit XOR key
    // Bag pockets — SaveBlock1-relative offsets + slot counts (from Lua tracker GameSettings)
    // Each slot = 4 bytes: u16 itemId + u16 quantity (quantity XOR-encrypted with 16-bit key for FR/LG/Emerald)
    val bagPocket_Items_offset: Int,
    val bagPocket_Items_size: Int,
    val bagPocket_Berries_offset: Int,
    val bagPocket_Berries_size: Int,
    // gTrainerBattleOpponent_A (u16): opponent trainer class index; 0 = wild (Lua tracker)
    val trainerBattleOpponent: Long = 0L,
    // gBattlerPartyIndexes: u8 array; [0]=playerSlot, [2]=enemySlot (Lua: Battle.Combatants.LeftOther)
    val gBattlerPartyIndexes: Long = 0L,
    // sSpecialFlags: u8; value 3 = catching tutorial active (Lua: Program.updateCatchingTutorial)
    val sSpecialFlags: Long = 0L,
    // Move validation — Lua Battle.lua gHitMarker / gMoveResultFlags / gBattleCommunication
    val gHitMarker: Long = 0L,
    val gMoveResultFlags: Long = 0L,
    val gBattleCommunication: Long = 0L,
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
    const val BASE_STATS_SPE:        Int = 3   // Speed is byte 3 in Gen III struct
    const val BASE_STATS_SPA:        Int = 4
    const val BASE_STATS_SPD:        Int = 5
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
    const val BMON_TYPE1:      Int = 0x21  // live type bytes — game engine updates these for
    const val BMON_TYPE2:      Int = 0x22  // Conversion, Conversion 2, Camouflage, Color Change
    const val BMON_STATUS:     Int = 0x28  // status1 (approximate; used for display only)

    // ── Party Pokemon unencrypted status (u32 at raw[0x50]) ──────────────────
    // Bit layout: bits 0-2 = sleep turns, bit 3 = PSN, bit 4 = BRN, bit 5 = FRZ, bit 6 = PAR, bit 7 = TOX
    const val OFF_STATUS:      Int = 0x50

    // ── gBattleResults offsets (from Lua tracker Program.Addresses) ──────────
    const val BATTLE_RESULTS_ENEMY_MOVE_OFFSET: Int = 0x24  // offsetBattleResultsEnemyMoveId

    // ── Move validation flags (Lua Program.Addresses) ────────────────────────
    // gHitMarker bit 19: HITMARKER_UNABLE_TO_USE_MOVE (paralysis, Truant, etc.)
    const val HITMARKER_UNABLE_TO_USE: Long = 0x80000L  // Lua: hitmarkerFlag80000
    // gMoveResultFlags combined mask: bits 0,3,5 = missed/no effect/failed
    const val MOVE_RESULT_NO_EFFECT: Int = 0x29         // Lua: moveResultsFlag29

    // ── Map header offset ────────────────────────────────────────────────────
    const val MAP_HEADER_LAYOUT_ID_OFFSET: Int = 0x12  // u16 mapLayoutId

    // =========================================================================
    // Per-game addresses
    // Version detection: read 1 byte at 0x080000BC (0=v1.0, 1=v1.1, 2=v1.2)
    // =========================================================================

    // FireRed English v1.0 (BPRE, version byte 0)
    // Addresses from Lua tracker: Pokemon FireRed v1.0.json
    private val FIRE_RED_V10 = GameAddresses(
        partyCount          = 0x02024029L,
        partyBase           = 0x02024284L,
        baseStatsTable      = 0x08254784L,
        levelUpLearnsets    = 0x0825D7B4L,  // Pokemon FireRed v1.0.json
        experienceTables    = 0x08253AE4L,
        enemyParty          = 0x0202402CL,
        battleTypeFlags     = 0x02022B4CL,
        battleMons          = 0x02023BE4L,
        battlersCount       = 0x02023BCCL,
        battleWeather       = 0x02023F1CL,
        sideStatuses        = 0x02023DDEL,
        sideTimers          = 0x02023DE4L,
        battleOutcome       = 0x02023E8AL,
        battleResults       = 0x3004F90L,   // gBattleResults (FR/LG all variants)
        gMapHeader          = 0x02036DFCL,
        saveBlock1Ptr       = 0x03005008L,
        saveBlock1IsPointer = true,
        gameStatsOffset     = 0x1200,
        gameFlagsOffset     = 0xEE0,        // gameFlagsOffset (all FR/LG variants)
        saveBlock2Ptr       = 0x0300500CL,  // gSaveBlock2ptr (English FR/LG)
        encryptionKeyOffset = 0xF20,        // EncryptionKeyOffset (all FR/LG variants)
        // Bag offsets from Lua tracker: Pokemon FireRed v1.0.json (same for all FR/LG)
        bagPocket_Items_offset  = 0x310,
        bagPocket_Items_size    = 0x2A,     // 42 slots
        bagPocket_Berries_offset = 0x54C,
        bagPocket_Berries_size  = 0x2B,     // 43 slots
        trainerBattleOpponent   = 0x020386AEL,  // gTrainerBattleOpponent_A (English FR/LG)
        gBattlerPartyIndexes    = 0x02023BCEL,  // gBattlerPartyIndexes (English FR/LG all versions)
        sSpecialFlags           = 0x020370E0L,  // sSpecialFlags (FR/LG): 3=catching tutorial (Lua tracker)
        gHitMarker              = 0x02023DD0L,  // gHitMarker (FR/LG all variants)
        gMoveResultFlags        = 0x02023DCCL,  // gMoveResultFlags (FR/LG all variants)
        gBattleCommunication    = 0x02023E82L,  // gBattleCommunication (FR/LG all variants)
    )

    // FireRed English v1.1 (BPRE, version byte 1)
    private val FIRE_RED_V11 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x082547F4L,
        levelUpLearnsets = 0x0825D824L,  // Pokemon FireRed v1.1.json
        experienceTables = 0x08253B54L,
    )

    // NatDex FireRed (any version) — addresses from CyanSMP64/NatDexExtension, GS.game == 3 block
    // ROM hack relocates gSpeciesInfo, gExperienceTables, SaveBlock pointers, and shifts some RAM structs.
    private val FIRE_RED_NATDEX = FIRE_RED_V10.copy(
        partyCount            = 0x0202402DL,   // GS.gPlayerPartyCount
        partyBase             = 0x02024288L,   // GS.pstats
        enemyParty            = 0x02024030L,   // GS.estats
        baseStatsTable        = 0x0826A5FCL,   // GS.gBaseStats (gSpeciesInfo relocated)
        levelUpLearnsets      = 0x0829050CL,   // gLevelUpLearnsets relocated by NatDex hack
        experienceTables      = 0x0826995CL,   // GS.gExperienceTables
        battleResults         = 0x03004BC0L,   // GS.gBattleResults
        gMapHeader            = 0x020363BCL,   // GS.gMapHeader
        saveBlock1Ptr         = 0x03004C38L,   // GS.gSaveBlock1ptr
        saveBlock2Ptr         = 0x03004C3CL,   // GS.gSaveBlock2ptr
        gameStatsOffset       = 0x1394,        // GS.gameStatsOffset
        gameFlagsOffset       = 0x1074,        // GS.gameFlagsOffset
        encryptionKeyOffset   = 0x400,         // GS.EncryptionKeyOffset
        trainerBattleOpponent = 0x02037C6EL,   // GS.gTrainerBattleOpponent_A
        sSpecialFlags         = 0x020366A0L,   // GS.sSpecialFlags
        // battle addresses (gBattleMons, sideStatuses, etc.) unchanged from vanilla FR
    )

    // FireRed non-English (Japanese uses 0x821118C, others vary)
    // Spanish=0x824FF4C, Italian=0x824D864, French=0x824EBD4, German=0x824EBD4-ish
    // For simplicity, we group by code suffix — added below in addressesFor

    // LeafGreen English v1.0
    private val LEAF_GREEN_V10 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x08254760L,
        levelUpLearnsets = 0x0825D794L,  // Pokemon LeafGreen v1.0.json
        experienceTables = 0x08253AC0L,
    )

    // LeafGreen English v1.1
    private val LEAF_GREEN_V11 = FIRE_RED_V10.copy(
        baseStatsTable   = 0x082547D0L,
        levelUpLearnsets = 0x0825D804L,  // Pokemon LeafGreen v1.1.json
        experienceTables = 0x08253B30L,
    )

    // Ruby v1.0
    // Addresses from Lua tracker: Pokemon Ruby v1.0.json
    // saveBlock2Ptr = 0L signals no encryption (Ruby/Sapphire per Lua tracker game==1 check).
    // saveBlock1IsPointer = false: gSaveBlock1 is a direct RAM address (0x2025734), not a pointer-to-pointer.
    private val RUBY_V10 = GameAddresses(
        partyCount          = 0x03004350L,  // gPlayerPartyCount (IWRAM)
        partyBase           = 0x03004360L,  // pstats (IWRAM)
        baseStatsTable      = 0x081FEC18L,
        levelUpLearnsets    = 0x08207BC8L,  // Pokemon Ruby v1.0.json
        experienceTables    = 0x081FDF78L,  // from Lua Ruby v1.0.json
        enemyParty          = 0x030045C0L,  // estats (IWRAM)
        battleTypeFlags     = 0x020239F8L,  // gBattleTypeFlags
        battleMons          = 0x02024A80L,  // gBattleMons
        battlersCount       = 0x02024A68L,  // gBattlersCount
        battleWeather       = 0x02024DB8L,  // gBattleWeather
        sideStatuses        = 0x02024C7AL,  // gSideStatuses
        sideTimers          = 0x02024C80L,  // gSideTimers
        battleOutcome       = 0x02024D26L,  // gBattleOutcome
        battleResults       = 0x030042E0L,  // gBattleResults (Ruby/Sapphire all variants)
        gMapHeader          = 0x0202E828L,
        saveBlock1Ptr       = 0x02025734L,  // gSaveBlock1 — direct address, not a pointer
        saveBlock1IsPointer = false,
        gameStatsOffset     = 0x1540,
        gameFlagsOffset     = 0x1220,       // gameFlagsOffset (all Ruby/Sapphire variants)
        saveBlock2Ptr       = 0L,           // No encryption for Ruby/Sapphire
        encryptionKeyOffset = 0,
        // Bag offsets from Lua tracker: Pokemon Ruby v1.0.json
        bagPocket_Items_offset  = 0x560,
        bagPocket_Items_size    = 0x14,     // 20 slots
        bagPocket_Berries_offset = 0x740,
        bagPocket_Berries_size  = 0x2E,     // 46 slots
        trainerBattleOpponent   = 0x0202FF5EL,  // gTrainerBattleOpponent_A
        gBattlerPartyIndexes    = 0x02024A6AL,  // gBattlerPartyIndexes (Ruby/Sapphire all versions)
        sSpecialFlags           = 0x0202E8E2L,  // sSpecialFlags (Ruby/Sapphire): 3=catching tutorial (Lua tracker)
        gHitMarker              = 0x02024C6CL,  // gHitMarker (Ruby/Sapphire all variants)
        gMoveResultFlags        = 0x02024C68L,  // gMoveResultFlags (Ruby/Sapphire all variants)
        gBattleCommunication    = 0x02024D1EL,  // gBattleCommunication (Ruby/Sapphire all variants)
    )

    // Ruby v1.1 / v1.2
    private val RUBY_V11 = RUBY_V10.copy(
        baseStatsTable   = 0x081FEC30L,
        levelUpLearnsets = 0x08207BE0L,  // Pokemon Ruby v1.1.json
    )

    // Sapphire v1.0 — same battle addresses as Ruby, different ROM addresses
    private val SAPPHIRE_V10 = RUBY_V10.copy(
        baseStatsTable   = 0x081FEBA8L,
        levelUpLearnsets = 0x08207B58L,  // Pokemon Sapphire v1.0.json
        experienceTables = 0x081FDF08L,  // from Lua Sapphire v1.0.json
    )

    // Sapphire v1.1 / v1.2
    private val SAPPHIRE_V11 = SAPPHIRE_V10.copy(
        baseStatsTable   = 0x081FEBC0L,
        levelUpLearnsets = 0x08207B70L,  // Pokemon Sapphire v1.1.json
    )

    // NatDex Emerald — addresses from CyanSMP64/NatDexExtension, GS.game == 2 block
    // Many battle/RAM structs shift by -4 bytes; ROM tables (gSpeciesInfo, gExperienceTables) relocated.
    // partyCount/partyBase/enemyParty not overridden by extension → unchanged from vanilla.

    // Emerald (single version)
    // Addresses from Lua tracker: Pokemon Emerald.json
    val EMERALD = GameAddresses(
        partyCount          = 0x020244E9L,
        partyBase           = 0x020244ECL,
        baseStatsTable      = 0x083203CCL,
        levelUpLearnsets    = 0x0832937CL,  // Pokemon Emerald.json
        experienceTables    = 0x082E82C4L,
        enemyParty          = 0x020244ECL,
        battleTypeFlags     = 0x02022FECL,
        battleMons          = 0x02024084L,
        battlersCount       = 0x0202406CL,  // gBattlersCount — was 0x02024074 (off by 8), fixed from Emerald.json
        battleWeather       = 0x020243CCL,
        sideStatuses        = 0x0202428EL,
        sideTimers          = 0x02024294L,
        battleOutcome       = 0x0202433AL, // gBattleOutcome from Emerald.json
        battleResults       = 0x3005D10L,  // gBattleResults from Emerald.json
        gMapHeader          = 0x02037318L,
        saveBlock1Ptr       = 0x03005D8CL,
        saveBlock1IsPointer = true,
        gameStatsOffset     = 0x159C,
        gameFlagsOffset     = 0x1270,       // gameFlagsOffset from Emerald.json
        saveBlock2Ptr       = 0x03005D90L, // gSaveBlock2ptr from Emerald.json
        encryptionKeyOffset = 0xAC,        // EncryptionKeyOffset from Emerald.json
        // Bag offsets from Lua tracker: Pokemon Emerald.json
        bagPocket_Items_offset  = 0x560,
        bagPocket_Items_size    = 0x1E,     // 30 slots
        bagPocket_Berries_offset = 0x790,
        bagPocket_Berries_size  = 0x2E,     // 46 slots
        trainerBattleOpponent   = 0x02038BCAL,  // gTrainerBattleOpponent_A from Emerald.json
        gBattlerPartyIndexes    = 0x0202406EL,  // gBattlerPartyIndexes from Emerald.json
        sSpecialFlags           = 0x020375FCL,  // sSpecialFlags (Emerald): 3=catching tutorial (Lua tracker)
        gHitMarker              = 0x02024280L,  // gHitMarker from Emerald.json
        gMoveResultFlags        = 0x0202427CL,  // gMoveResultFlags from Emerald.json
        gBattleCommunication    = 0x02024332L,  // gBattleCommunication from Emerald.json
    )

    private val EMERALD_NATDEX = EMERALD.copy(
        baseStatsTable        = 0x08323840L,   // GS.gBaseStats (gSpeciesInfo relocated)
        levelUpLearnsets      = 0x08349750L,   // gLevelUpLearnsets relocated by NatDex hack
        experienceTables      = 0x08322BA0L,   // GS.gExperienceTables
        battleTypeFlags       = 0x02022FE8L,   // GS.gBattleTypeFlags (-4)
        battleMons            = 0x02024080L,   // GS.gBattleMons (-4)
        battlersCount         = 0x02024068L,   // GS.gBattlersCount (-4)
        battleOutcome         = 0x02024336L,   // GS.gBattleOutcome (-4)
        battleWeather         = 0x020243C8L,   // GS.gBattleWeather (-4)
        gMapHeader            = 0x020369D0L,   // GS.gMapHeader
        sSpecialFlags         = 0x02036CB4L,   // GS.sSpecialFlags
        trainerBattleOpponent = 0x02038282L,   // GS.gTrainerBattleOpponent_A
        gBattlerPartyIndexes  = 0x0202406AL,   // GS.gBattlerPartyIndexes (-4)
        gameStatsOffset       = 0x1764,        // GS.gameStatsOffset
        gameFlagsOffset       = 0x1438,        // GS.gameFlagsOffset
        encryptionKeyOffset   = 0x170,         // GS.EncryptionKeyOffset
        battleResults         = 0x03004C40L,   // GS.gBattleResults
        saveBlock1Ptr         = 0x03004CBCL,   // GS.gSaveBlock1ptr
        saveBlock2Ptr         = 0x03004CC0L,   // GS.gSaveBlock2ptr
        // partyCount/partyBase/enemyParty/sideStatuses/sideTimers — not overridden, keep vanilla
    )

    /**
     * Returns the correct addresses for [game] and [romVersion] (byte from 0x080000BC).
     * [gameCode] is the 4-char game code used to detect non-English variants.
     * [isNatDex] selects NatDex ROM hack addresses when true (FireRed and Emerald only).
     */
    fun addressesFor(game: GameVersion, romVersion: Int = 0, gameCode: String = "", isNatDex: Boolean = false): GameAddresses? = when (game) {
        GameVersion.FIRE_RED -> when {
            isNatDex -> FIRE_RED_NATDEX
            // Non-English FR use gSaveBlock2ptr = 0x03004F5C (per Lua tracker JSONs)
            gameCode == "BPRS" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824FF4CL, saveBlock2Ptr = 0x03004F5CL) // Spanish
            gameCode == "BPRI" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824D864L, saveBlock2Ptr = 0x03004F5CL) // Italian
            gameCode == "BPRF" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824EBD4L, saveBlock2Ptr = 0x03004F5CL) // French
            gameCode == "BPRD" -> FIRE_RED_V10.copy(baseStatsTable = 0x0824EBD4L, saveBlock2Ptr = 0x03004F5CL) // German (approx)
            gameCode == "BPRJ" -> FIRE_RED_V10.copy(baseStatsTable = 0x0821118CL, saveBlock2Ptr = 0x0300504CL, trainerBattleOpponent = 0x0203860EL, gBattlerPartyIndexes = 0x02023B2EL) // Japanese
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
        GameVersion.EMERALD    -> if (isNatDex) EMERALD_NATDEX else EMERALD
        GameVersion.UNKNOWN    -> null
    }
}
