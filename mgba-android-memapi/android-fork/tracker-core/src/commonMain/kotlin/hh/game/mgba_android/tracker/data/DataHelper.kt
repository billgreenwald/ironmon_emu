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
    // MaxFR/MaxEM ROM hack: relocated move table address (0L = vanilla, no per-move category)
    val gBattleMoves: Long = 0L,
    // Whether Fairy type (ID 18) is active — true for MaxFR/MaxEM hacks
    val hasFairy: Boolean = false,
    // Gen4/Gen5 Pokemon data loaded from Lua for MaxFR variants; empty for vanilla games
    val extPokemonMap: Map<Int, MaxExtPokemon> = emptyMap(),
    // Per-mon memory layout (party struct + base-stats field offsets). Vanilla Gen III by
    // default; NatDex 1.2.x relocates these and exports the new values in its ROM meta-table
    // (read via NatDexMetaAddressReader). Everything that decodes a mon reads offsets from here.
    val monLayout: MonLayout = MonLayout(),
)

/**
 * Per-mon memory layout. Defaults are the vanilla Gen III layout (matches the [DataHelper]
 * `OFF_*` / `BASE_STATS_*` constants). NatDex 1.2.x (a pokeemerald-expansion base) expands the
 * Pokémon struct — the encrypted substructures and the appended battle-stats block shift, and
 * `gSpeciesInfo` grows — so 1.2.x exports every relocated offset in a ROM meta-table. We read
 * them at runtime (see [NatDexMetaAddressReader]) rather than porting per-version, exactly as
 * upstream NatDexExtension.lua `updateProgramAddresses()` does. Substructure INTERNAL offsets
 * (growth species@+0, exp@+4, moves, IV word, …) are unchanged — only the substruct START moves.
 */
data class MonLayout(
    val structSize: Int = 100,        // sizeofPokemonStruct (vanilla 0x64)
    val substruct: Int = 0x20,        // offsetPokemonSubstruct — start of the 48-byte encrypted block
    val status: Int = 0x50,           // offsetPokemonStatus (u32, unencrypted)
    val statsLvCurHp: Int = 0x54,     // level @+0 (u8), currentHp @+2 (u16)
    val statsMaxHpAtk: Int = 0x58,    // maxHp @+0 (u16), attack @+2 (u16)
    val statsDefSpe: Int = 0x5C,      // defense @+0 (u16), speed @+2 (u16)
    val statsSpaSpd: Int = 0x60,      // spAtk @+0 (u16), spDef @+2 (u16)
    // gSpeciesInfo entry layout
    val baseStatsEntrySize: Int = 28, // sizeofBaseStatsPokemon (vanilla 0x1C)
    val bsBaseStats: Int = 0,         // offsetBaseStats: HP,Atk,Def,Spe,SpA,SpD (6×u8, this order)
    val bsTypes: Int = 6,             // offsetTypes: type1 @+0, type2 @+1
    val bsGenderRatio: Int = 16,      // offsetGenderRatio (vanilla 0x10)
    val bsGrowthRate: Int = 19,       // offsetGrowthRateIndex (vanilla 0x13) — exp group
    val bsAbilities: Int = 22,        // offsetAbilities: ability1 @+0, ability2 @+abilitySize
    val abilitySize: Int = 1,         // sizeofAbilityInBytes (u16 in the expansion → 2)
    // gBattleMons (BattlePokemon) layout — species@0x00 and moves@0x0C are stable; the expansion
    // grows the struct and relocates statStages/types, so the slot stride + those offsets are exported.
    val battleMonSize: Int = 0x58,        // sizeofBattlePokemon — per-battler stride
    val bmonStatStages: Int = 0x18,       // offsetBattlePokemonStatStages: [HP,Atk,Def,Spe,SpA,SpD,Acc,Eva]
    val bmonTypes: Int = 0x21,            // offsetBattlePokemonTypes: type1 @+0, type2 @+1
    val bmonDoublesPartner: Int = 0xB0,   // offsetBattlePokemonDoublesPartner — left→right battler delta
    // Level-up learnset layout. Vanilla packs each entry as a 2-byte word ((lvl<<9)|move); the
    // expansion widens it. moveId/level are BIT offsets+sizes (getbits); entrySize/ptrStride are bytes.
    val learnsetPtrStride: Int = 4,       // sizeofLevelUpLearnset — pointer-array entry size
    val learnsetEntrySize: Int = 2,       // sizeofLevelUpMove — bytes per learnset entry
    val learnsetMoveIdBitOff: Int = 0,    // offsetLevelUpMoveId (bits)
    val learnsetMoveIdBits: Int = 9,      // sizeofLevelUpMoveId (bits)
    val learnsetLevelBitOff: Int = 9,     // offsetLevelUpMoveLv (bits)
    val learnsetLevelBits: Int = 7,       // sizeofLevelUpMoveLv (bits)
    val learnsetEndFlag: Long = 0xFFFF,   // endFlagLevelUp — sentinel entry value
) {
    // Party-stat byte offsets derived from the packed pair offsets above.
    val level: Int      get() = statsLvCurHp
    val currentHp: Int  get() = statsLvCurHp + 2
    val maxHp: Int      get() = statsMaxHpAtk
    val attack: Int     get() = statsMaxHpAtk + 2
    val defense: Int    get() = statsDefSpe
    val speed: Int      get() = statsDefSpe + 2
    val spAtk: Int      get() = statsSpaSpd
    val spDef: Int      get() = statsSpaSpd + 2
    // gSpeciesInfo field byte offsets.
    val bsHp: Int    get() = bsBaseStats + 0
    val bsAtk: Int   get() = bsBaseStats + 1
    val bsDef: Int   get() = bsBaseStats + 2
    val bsSpe: Int   get() = bsBaseStats + 3
    val bsSpa: Int   get() = bsBaseStats + 4
    val bsSpd: Int   get() = bsBaseStats + 5
    val bsType1: Int get() = bsTypes + 0
    val bsType2: Int get() = bsTypes + 1
    val bsAbility1: Int get() = bsAbilities
    val bsAbility2: Int get() = bsAbilities + abilitySize
    // BattlePokemon derived offsets.
    val bmonType1: Int get() = bmonTypes + 0
    val bmonType2: Int get() = bmonTypes + 1
}

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
    // ── gBattleTypeFlags bits (Gen III engine constants, shared across all games) ──
    const val BATTLE_TYPE_DOUBLE:  Int = 0x01    // (1 << 0)
    const val BATTLE_TYPE_TRAINER: Int = 0x08    // (1 << 3)
    // (1 << 15): Emerald/RSE = two SEPARATE opposing trainers (optional overworld double
    // battle). FRLG reuses this bit as BATTLE_TYPE_GHOST (wild-only), so only meaningful
    // when combined with a trainer double battle. Source: Lua RouteData.lua flag list.
    const val BATTLE_TYPE_TWO_OPPONENTS: Int = 0x8000

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
    // ROM hack relocates gSpeciesInfo, gExperienceTables, and shifts many RAM structs.
    // SaveBlock pointer variables also moved: gSaveBlock1ptr = 0x03004C38, gSaveBlock2ptr = 0x03004C3C
    // SaveBlock1/2 internal layout also changed: gameStatsOffset and encryptionKeyOffset are NatDex values.
    private val FIRE_RED_NATDEX = FIRE_RED_V10.copy(
        partyCount            = 0x0202402DL,   // GS.gPlayerPartyCount
        partyBase             = 0x02024288L,   // GS.pstats
        enemyParty            = 0x02024030L,   // GS.estats
        baseStatsTable        = 0x0826A5FCL,   // GS.gBaseStats (gSpeciesInfo relocated)
        levelUpLearnsets      = 0x0829050CL,   // gLevelUpLearnsets relocated by NatDex hack
        experienceTables      = 0x0826995CL,   // GS.gExperienceTables
        battleResults         = 0x03004BC0L,   // GS.gBattleResults
        gMapHeader            = 0x020363BCL,   // GS.gMapHeader
        saveBlock1Ptr         = 0x03004C38L,   // GS.gSaveBlock1ptr (moved by NatDex hack)
        saveBlock2Ptr         = 0x03004C3CL,   // GS.gSaveBlock2ptr (moved by NatDex hack)
        gameStatsOffset       = 0x1394,        // GS.gameStatsOffset (SaveBlock1 layout changed)
        gameFlagsOffset       = 0x1074,        // GS.gameFlagsOffset
        encryptionKeyOffset   = 0x400,         // GS.EncryptionKeyOffset (SaveBlock2 layout changed)
        trainerBattleOpponent = 0x02037C6EL,   // GS.gTrainerBattleOpponent_A
        sSpecialFlags         = 0x020366A0L,   // GS.sSpecialFlags
        // NatDex expands Items pocket from 42 to 120 slots, shifting all downstream pockets
        bagPocket_Items_size     = 0x78,       // 120 slots (vanilla: 0x2A = 42)
        bagPocket_Berries_offset = 0x684,      // 0x310 + 120×4 + 30×4 + 13×4 + 58×4
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
        enemyParty          = 0x02024744L,  // estats from Pokemon Emerald.json (was incorrectly set to pstats/player party)
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
        // NatDex expands Items pocket from 30 to 120 slots, shifting all downstream pockets
        bagPocket_Items_size     = 0x78,       // 120 slots (vanilla: 0x1E = 30)
        bagPocket_Berries_offset = 0x8F8,      // 0x560 + 120×4 + 30×4 + 16×4 + 64×4
        // partyCount/partyBase/enemyParty/sideStatuses/sideTimers — not overridden, keep vanilla
    )

    // MaxFR/MaxEM ROM hack addresses are loaded at runtime from bundled asset JSON files
    // via MaxFrAddressLoader. See app/src/main/assets/maxdata/ for the source files.

    /**
     * Returns vanilla (non-MaxFR) addresses for [game] and [romVersion].
     * MaxFR/MaxEM variants are handled separately by MaxFrAddressLoader (asset JSON files).
     * [gameCode] selects non-English FR variants. [isNatDex] selects NatDex ROM hack addresses.
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
        GameVersion.EMERALD -> when {
            isNatDex -> EMERALD_NATDEX
            else     -> EMERALD
        }
        GameVersion.UNKNOWN -> null
    }

    /** Reads power/type/accuracy/PP/category for a move from the ROM gBattleMoves table.
     *  Move struct is 12 bytes: byte0=effect, 1=power, 2=type, 3=accuracy, 4=PP, 8=flags.
     *  Category at flags bits 6-7 (Gen IV phys/spec split patch; 0 if vanilla).
     *  Returns null if address unavailable or read fails. */
    data class RomMoveStats(val power: Int, val type: Int, val accuracy: Int, val pp: Int, val category: Int)

    fun readMoveStatsFromRom(reader: (Long, Int) -> ByteArray?, gBattleMovesAddr: Long, moveId: Int): RomMoveStats? {
        if (gBattleMovesAddr == 0L || moveId <= 0) return null
        val addr = gBattleMovesAddr + moveId.toLong() * 12L
        val bytes = reader(addr, 9) ?: return null
        val power    = bytes[1].toInt() and 0xFF
        val type     = bytes[2].toInt() and 0xFF
        val accuracy = bytes[3].toInt() and 0xFF
        val pp       = bytes[4].toInt() and 0xFF
        val category = (bytes[8].toInt() and 0xFF) ushr 6 and 3
        return RomMoveStats(power, type, accuracy, pp, category)
    }

    /** Reads the Gen IV per-move category from the ROM gBattleMoves table.
     *  Move struct is 12 bytes; byte 8 = flags with category at bits 5-6.
     *  Returns 1=Physical, 2=Special, 3=Status, 0=unknown/unavailable. */
    fun readMoveCategory(reader: (Long, Int) -> ByteArray?, gBattleMovesAddr: Long, moveId: Int): Int {
        if (gBattleMovesAddr == 0L || moveId <= 0) return 0
        val addr = gBattleMovesAddr + moveId.toLong() * 12L
        val bytes = reader(addr, 9) ?: return 0
        val flags = bytes[8].toInt() and 0xFF
        return (flags ushr 6) and 3
    }
}
