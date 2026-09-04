package hh.game.mgba_android.tracker.data

/**
 * NatDex 1.2.x+ (CyanSMP64/NatDexExtension) exports all of its (relocated) addresses in a metadata
 * table baked into the ROM header region (0x08000150–0x08000318). The extension itself builds its
 * address set this way — `NatDexExtension.lua updateGameSettings()` does `Memory.read32(0x08000278)`
 * etc. The meta-offsets are **byte-identical across v1.2.0 and v1.2.1** and game-agnostic (FireRed and
 * Emerald read the SAME offsets; the ROM holds the game-appropriate value), so they are a stable,
 * version-proof interface: future 1.2.x patches move tables freely while these offsets stay put.
 *
 * We read the table exactly as the extension does — no per-version porting, no bundled JSON. Every
 * field [GameAddresses] needs is present in the table. The values that are byte offsets
 * (gameStatsOffset, gameFlagsOffset, encryptionKeyOffset, bag pockets) are read the same way and
 * stored as Int.
 */
object NatDexMetaAddressReader {

    // ── read32 meta-offsets (hold a runtime address, or an offset value) ─────────────────────────
    private const val M_GAME_FLAGS_OFFSET       = 0x08000150L
    private const val M_BASE_STATS_TABLE        = 0x080001BCL  // gSpeciesInfo
    private const val M_G_BATTLE_MOVES          = 0x080001CCL
    private const val M_BATTLE_TYPE_FLAGS       = 0x0800020CL
    private const val M_BATTLERS_COUNT          = 0x08000218L
    private const val M_G_BATTLER_PARTY_INDEXES = 0x0800021CL
    private const val M_BATTLE_MONS             = 0x08000228L
    private const val M_G_MOVE_RESULT_FLAGS     = 0x08000240L
    private const val M_G_HIT_MARKER            = 0x08000244L
    private const val M_SIDE_STATUSES           = 0x08000248L
    private const val M_SIDE_TIMERS             = 0x0800024CL
    private const val M_G_BATTLE_COMMUNICATION  = 0x0800025CL
    private const val M_BATTLE_OUTCOME          = 0x08000260L
    private const val M_BATTLE_WEATHER          = 0x08000264L
    private const val M_PARTY_COUNT             = 0x08000278L
    private const val M_PARTY_BASE              = 0x0800027CL  // pstats / gPlayerParty
    private const val M_ENEMY_PARTY             = 0x08000280L  // estats
    private const val M_G_MAP_HEADER            = 0x08000284L
    private const val M_S_SPECIAL_FLAGS         = 0x0800028CL
    private const val M_TRAINER_BATTLE_OPPONENT = 0x08000294L
    private const val M_GAME_STATS_OFFSET       = 0x080002B4L
    private const val M_BAG_ITEMS_OFFSET        = 0x080002BCL
    private const val M_BAG_BERRIES_OFFSET      = 0x080002CCL
    private const val M_ENCRYPTION_KEY_OFFSET   = 0x080002D0L
    private const val M_BATTLE_RESULTS          = 0x080002D8L
    private const val M_SAVE_BLOCK1_PTR         = 0x080002E0L
    private const val M_SAVE_BLOCK2_PTR         = 0x080002E4L
    private const val M_EXPERIENCE_TABLES       = 0x08000308L
    private const val M_LEVEL_UP_LEARNSETS      = 0x0800030CL

    // ── read8 meta-offsets (bag pocket slot counts) ──────────────────────────────────────────────
    private const val M_BAG_ITEMS_SIZE   = 0x080001E4L
    private const val M_BAG_BERRIES_SIZE = 0x080001E8L

    // ── read16 meta-offsets: per-mon struct + gSpeciesInfo layout ─────────────────────────────────
    // The expansion relocates these vs vanilla Gen III, so 1.2.x exports them too. Mirrors
    // NatDexExtension.lua v1.2.1 updateProgramAddresses(). See [MonLayout] for what each replaces.
    private const val M_OFF_GROWTH_RATE      = 0x080003F0L  // offsetGrowthRateIndex (exp group)
    private const val M_OFF_SUBSTRUCT        = 0x08000414L  // offsetPokemonSubstruct
    private const val M_OFF_STATS_LVCURHP    = 0x08000418L  // offsetPokemonStatsLvCurHp
    private const val M_OFF_STATS_MAXHPATK   = 0x0800041AL  // offsetPokemonStatsMaxHpAtk
    private const val M_OFF_STATS_DEFSPE     = 0x0800041CL  // offsetPokemonStatsDefSpe
    private const val M_OFF_STATS_SPASPD     = 0x0800041EL  // offsetPokemonStatsSpaSpd
    private const val M_OFF_STATUS           = 0x08000416L  // offsetPokemonStatus
    private const val M_SIZEOF_BASESTATS     = 0x08000428L  // sizeofBaseStatsPokemon
    private const val M_SIZEOF_POKEMON       = 0x08000442L  // sizeofPokemonStruct
    private const val M_BS_OFF_BASESTATS     = 0x08000468L  // PokemonData.offsetBaseStats
    private const val M_BS_OFF_TYPES         = 0x0800046AL  // PokemonData.offsetTypes
    private const val M_BS_OFF_GENDER        = 0x08000470L  // PokemonData.offsetGenderRatio
    private const val M_BS_OFF_ABILITIES     = 0x08000474L  // PokemonData.offsetAbilities
    private const val M_SIZEOF_ABILITY       = 0x0800047CL  // PokemonData.sizeofAbilityInBytes

    // GBA memory regions used to validate the table is really present.
    private val ROM   = 0x08000000L..0x09FFFFFFL
    private val EWRAM = 0x02000000L..0x0203FFFFL
    private val IWRAM = 0x03000000L..0x03007FFFL

    /**
     * True if the ROM meta-table looks valid — i.e. this is a NatDex 1.2.x+ ROM. Checks that several
     * exported pointers land in their expected memory regions; a random (vanilla/MaxFR) ROM
     * satisfying all of these at once is astronomically unlikely, so this avoids false positives.
     */
    fun isValidMetaTable(reader: (Long, Int) -> ByteArray?): Boolean {
        val baseStats   = u32(reader, M_BASE_STATS_TABLE) ?: return false
        val battleMoves = u32(reader, M_G_BATTLE_MOVES)   ?: return false
        val partyBase   = u32(reader, M_PARTY_BASE)       ?: return false
        val battleMons  = u32(reader, M_BATTLE_MONS)      ?: return false
        val saveBlock1  = u32(reader, M_SAVE_BLOCK1_PTR)  ?: return false
        return baseStats in ROM && battleMoves in ROM &&
               partyBase in EWRAM && battleMons in EWRAM &&
               saveBlock1 in IWRAM
    }

    /**
     * Reads the full address set from the ROM meta-table. Returns null if any read fails (core not
     * ready yet) — callers should retry on a later poll rather than caching a partial set.
     */
    fun read(reader: (Long, Int) -> ByteArray?): GameAddresses? {
        return GameAddresses(
            partyCount            = u32(reader, M_PARTY_COUNT)             ?: return null,
            partyBase             = u32(reader, M_PARTY_BASE)              ?: return null,
            baseStatsTable        = u32(reader, M_BASE_STATS_TABLE)        ?: return null,
            levelUpLearnsets      = u32(reader, M_LEVEL_UP_LEARNSETS)      ?: return null,
            experienceTables      = u32(reader, M_EXPERIENCE_TABLES)       ?: return null,
            enemyParty            = u32(reader, M_ENEMY_PARTY)             ?: return null,
            battleTypeFlags       = u32(reader, M_BATTLE_TYPE_FLAGS)       ?: return null,
            battleMons            = u32(reader, M_BATTLE_MONS)             ?: return null,
            battlersCount         = u32(reader, M_BATTLERS_COUNT)          ?: return null,
            battleWeather         = u32(reader, M_BATTLE_WEATHER)          ?: return null,
            sideStatuses          = u32(reader, M_SIDE_STATUSES)           ?: return null,
            sideTimers            = u32(reader, M_SIDE_TIMERS)             ?: return null,
            battleOutcome         = u32(reader, M_BATTLE_OUTCOME)          ?: return null,
            battleResults         = u32(reader, M_BATTLE_RESULTS)          ?: return null,
            gMapHeader            = u32(reader, M_G_MAP_HEADER)            ?: return null,
            saveBlock1Ptr         = u32(reader, M_SAVE_BLOCK1_PTR)         ?: return null,
            saveBlock1IsPointer   = true,
            gameStatsOffset       = (u32(reader, M_GAME_STATS_OFFSET)      ?: return null).toInt(),
            gameFlagsOffset       = (u32(reader, M_GAME_FLAGS_OFFSET)      ?: return null).toInt(),
            saveBlock2Ptr         = u32(reader, M_SAVE_BLOCK2_PTR)         ?: return null,
            encryptionKeyOffset   = (u32(reader, M_ENCRYPTION_KEY_OFFSET)  ?: return null).toInt(),
            bagPocket_Items_offset   = (u32(reader, M_BAG_ITEMS_OFFSET)    ?: return null).toInt(),
            bagPocket_Items_size     = u8(reader, M_BAG_ITEMS_SIZE)        ?: return null,
            bagPocket_Berries_offset = (u32(reader, M_BAG_BERRIES_OFFSET)  ?: return null).toInt(),
            bagPocket_Berries_size   = u8(reader, M_BAG_BERRIES_SIZE)      ?: return null,
            trainerBattleOpponent = u32(reader, M_TRAINER_BATTLE_OPPONENT) ?: return null,
            gBattlerPartyIndexes  = u32(reader, M_G_BATTLER_PARTY_INDEXES) ?: return null,
            sSpecialFlags         = u32(reader, M_S_SPECIAL_FLAGS)         ?: return null,
            gHitMarker            = u32(reader, M_G_HIT_MARKER)            ?: return null,
            gMoveResultFlags      = u32(reader, M_G_MOVE_RESULT_FLAGS)     ?: return null,
            gBattleCommunication  = u32(reader, M_G_BATTLE_COMMUNICATION)  ?: return null,
            gBattleMoves          = u32(reader, M_G_BATTLE_MOVES)          ?: return null,
            hasFairy              = true,
            extPokemonMap         = emptyMap(),
            monLayout             = MonLayout(
                structSize         = u16(reader, M_SIZEOF_POKEMON)     ?: return null,
                substruct          = u16(reader, M_OFF_SUBSTRUCT)      ?: return null,
                status             = u16(reader, M_OFF_STATUS)         ?: return null,
                statsLvCurHp       = u16(reader, M_OFF_STATS_LVCURHP)  ?: return null,
                statsMaxHpAtk      = u16(reader, M_OFF_STATS_MAXHPATK) ?: return null,
                statsDefSpe        = u16(reader, M_OFF_STATS_DEFSPE)   ?: return null,
                statsSpaSpd        = u16(reader, M_OFF_STATS_SPASPD)   ?: return null,
                baseStatsEntrySize = u16(reader, M_SIZEOF_BASESTATS)   ?: return null,
                bsBaseStats        = u16(reader, M_BS_OFF_BASESTATS)   ?: return null,
                bsTypes            = u16(reader, M_BS_OFF_TYPES)       ?: return null,
                bsGenderRatio      = u16(reader, M_BS_OFF_GENDER)      ?: return null,
                bsGrowthRate       = u16(reader, M_OFF_GROWTH_RATE)    ?: return null,
                bsAbilities        = u16(reader, M_BS_OFF_ABILITIES)   ?: return null,
                abilitySize        = u16(reader, M_SIZEOF_ABILITY)     ?: return null,
            ),
        )
    }

    private fun u32(reader: (Long, Int) -> ByteArray?, addr: Long): Long? {
        val b = reader(addr, 4) ?: return null
        if (b.size < 4) return null
        return ((b[0].toInt() and 0xFF).toLong()) or
               ((b[1].toInt() and 0xFF).toLong() shl 8) or
               ((b[2].toInt() and 0xFF).toLong() shl 16) or
               ((b[3].toInt() and 0xFF).toLong() shl 24)
    }

    private fun u16(reader: (Long, Int) -> ByteArray?, addr: Long): Int? {
        val b = reader(addr, 2) ?: return null
        if (b.size < 2) return null
        return (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8)
    }

    private fun u8(reader: (Long, Int) -> ByteArray?, addr: Long): Int? {
        val b = reader(addr, 1) ?: return null
        if (b.isEmpty()) return null
        return b[0].toInt() and 0xFF
    }
}
