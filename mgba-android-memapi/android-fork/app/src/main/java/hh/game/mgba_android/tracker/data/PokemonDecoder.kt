package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.Gender
import hh.game.mgba_android.tracker.models.MoveData
import hh.game.mgba_android.tracker.models.PokemonData
import hh.game.mgba_android.tracker.tables.GenIIICharMap
import hh.game.mgba_android.tracker.tables.MoveStatsTable

object PokemonDecoder {

    // sHiddenPowerType[] from pret pokefirered source — maps HP index 0-15 to ROM type IDs
    private val HP_TYPE_ROM_IDS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17)

    internal val SUBSTRUCTURE_ORDER: Array<IntArray> = arrayOf(
        intArrayOf(0, 1, 2, 3), // 0:  GAEM
        intArrayOf(0, 1, 3, 2), // 1:  GAME
        intArrayOf(0, 2, 1, 3), // 2:  GEAM
        intArrayOf(0, 3, 1, 2), // 3:  GEMA
        intArrayOf(0, 2, 3, 1), // 4:  GMAE
        intArrayOf(0, 3, 2, 1), // 5:  GMEA
        intArrayOf(1, 0, 2, 3), // 6:  AGEM
        intArrayOf(1, 0, 3, 2), // 7:  AGME
        intArrayOf(2, 0, 1, 3), // 8:  AEGM
        intArrayOf(3, 0, 1, 2), // 9:  AEMG
        intArrayOf(2, 0, 3, 1), // 10: AMGE
        intArrayOf(3, 0, 2, 1), // 11: AMEG
        intArrayOf(1, 2, 0, 3), // 12: EGAM
        intArrayOf(1, 3, 0, 2), // 13: EGMA
        intArrayOf(2, 1, 0, 3), // 14: EAGM
        intArrayOf(3, 1, 0, 2), // 15: EAMG
        intArrayOf(2, 3, 0, 1), // 16: EMGA
        intArrayOf(3, 2, 0, 1), // 17: EMAG
        intArrayOf(1, 2, 3, 0), // 18: MGAE
        intArrayOf(1, 3, 2, 0), // 19: MAGE
        intArrayOf(2, 1, 3, 0), // 20: MEGA
        intArrayOf(3, 1, 2, 0), // 21: MEAG
        intArrayOf(2, 3, 1, 0), // 22: MGEA
        intArrayOf(3, 2, 1, 0), // 23: MAEG
    )

    fun decode(
        slot: Int,
        raw: ByteArray,
        nameTable: (Int) -> String,
        moveTable: (Int) -> String,
        baseStatsReader: ((speciesId: Int) -> ByteArray?)? = null,
    ): PokemonData? {
        require(raw.size >= 100) { "Raw bytes must be at least 100, got ${raw.size}" }

        val personality = raw.u32(DataHelper.OFF_PERSONALITY)
        val otId        = raw.u32(DataHelper.OFF_OT_ID)

        val xorKey = personality xor otId
        val decrypted = ByteArray(48)
        for (i in 0 until 12) {
            val word = raw.u32(DataHelper.OFF_ENCRYPTED + i * 4) xor xorKey
            decrypted[i * 4 + 0] = (word and 0xFF).toByte()
            decrypted[i * 4 + 1] = ((word shr 8) and 0xFF).toByte()
            decrypted[i * 4 + 2] = ((word shr 16) and 0xFF).toByte()
            decrypted[i * 4 + 3] = ((word shr 24) and 0xFF).toByte()
        }

        val orderIndex = (personality % 24).toInt()
        val order      = SUBSTRUCTURE_ORDER[orderIndex]

        // Substructure offsets within 48-byte decrypted block
        val growthOffset  = order[0] * 12
        val attacksOffset = order[1] * 12
        val evOffset      = order[2] * 12
        val miscOffset    = order[3] * 12

        // ── Growth ────────────────────────────────────────────────────────────
        val speciesId  = decrypted.u16(growthOffset + DataHelper.GROWTH_SPECIES)
        val heldItemId = decrypted.u16(growthOffset + DataHelper.GROWTH_ITEM)
        val experience = decrypted.u32(growthOffset + DataHelper.GROWTH_EXP).toInt()
        // Friendship at growthOffset+9 (byte after ppBonuses at +8) — matches Lua: Utils.getbits(growth3, 8, 8)
        val friendship = decrypted[growthOffset + 9].toInt() and 0xFF

        if (speciesId == 0) return null

        // ── Attacks ───────────────────────────────────────────────────────────
        val move1Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE1)
        val move2Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE2)
        val move3Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE3)
        val move4Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE4)
        val pp1     = decrypted[attacksOffset + DataHelper.ATK_PP1].toInt() and 0xFF
        val pp2     = decrypted[attacksOffset + DataHelper.ATK_PP2].toInt() and 0xFF
        val pp3     = decrypted[attacksOffset + DataHelper.ATK_PP3].toInt() and 0xFF
        val pp4     = decrypted[attacksOffset + DataHelper.ATK_PP4].toInt() and 0xFF

        // ── Misc substructure ─────────────────────────────────────────────────
        val pokerusRaw = decrypted.u16(miscOffset + DataHelper.MISC_POKERUS)
        val ivWord     = decrypted.u32(miscOffset + DataHelper.MISC_IV_ABILITY)
        val hasPokerus = pokerusRaw != 0
        val abilityIndex = ((ivWord ushr 31) and 1).toInt()

        // IVs (5 bits each) from ivWord: HP, Atk, Def, Spe, SpA, SpD
        val ivHp  = (ivWord ushr  0).toInt() and 0x1F
        val ivAtk = (ivWord ushr  5).toInt() and 0x1F
        val ivDef = (ivWord ushr 10).toInt() and 0x1F
        val ivSpe = (ivWord ushr 15).toInt() and 0x1F
        val ivSpA = (ivWord ushr 20).toInt() and 0x1F
        val ivSpD = (ivWord ushr 25).toInt() and 0x1F

        // Hidden Power type — Gen III formula → ROM type ID
        // Formula yields index 0-15 → map to sHiddenPowerType[] from pret source
        // (Fighting=1, Flying=2, Poison=3, Ground=4, Rock=5, Bug=6, Ghost=7, Steel=8,
        //  Fire=10, Water=11, Grass=12, Electric=13, Psychic=14, Ice=15, Dragon=16, Dark=17)
        val hpTypeIndex = ((ivHp and 1) + 2*(ivAtk and 1) + 4*(ivDef and 1) +
            8*(ivSpe and 1) + 16*(ivSpA and 1) + 32*(ivSpD and 1)) * 15 / 63
        val hiddenPowerType = HP_TYPE_ROM_IDS[hpTypeIndex]

        // ── Effort substructure (EVs) ─────────────────────────────────────────
        val effort1 = decrypted.u32(evOffset + 0)
        val effort2 = decrypted.u32(evOffset + 4)
        val evHp  = (effort1 ushr  0).toInt() and 0xFF
        val evAtk = (effort1 ushr  8).toInt() and 0xFF
        val evDef = (effort1 ushr 16).toInt() and 0xFF
        val evSpe = (effort1 ushr 24).toInt() and 0xFF
        val evSpA = (effort2 ushr  0).toInt() and 0xFF
        val evSpD = (effort2 ushr  8).toInt() and 0xFF

        // ── Nickname — unencrypted at raw[0x08], 10 bytes, 0xFF = terminator ────
        // Matches Lua Program.lua: for i=0, sizeofPokemonNickname-1 do readbyte(startAddress + 8 + i)
        val nickname = buildString {
            for (i in 0 until 10) {
                val b = raw[0x08 + i].toInt() and 0xFF
                if (b == 0xFF) break
                append(GenIIICharMap.get(b))
            }
        }.trim()

        // ── Status condition — unencrypted u32 at raw[0x50] ──────────────────
        val statusCondition = raw[DataHelper.OFF_STATUS].toInt() and 0xFF

        // ── Unencrypted in-battle stats ───────────────────────────────────────
        val level     = raw[DataHelper.OFF_LEVEL].toInt() and 0xFF
        val currentHp = raw.u16(DataHelper.OFF_CURRENT_HP)
        val maxHp     = raw.u16(DataHelper.OFF_MAX_HP)
        val attack    = raw.u16(DataHelper.OFF_ATTACK)
        val defense   = raw.u16(DataHelper.OFF_DEFENSE)
        val speed     = raw.u16(DataHelper.OFF_SPEED)
        val spAtk     = raw.u16(DataHelper.OFF_SP_ATK)
        val spDef     = raw.u16(DataHelper.OFF_SP_DEF)

        // ── Computed fields ───────────────────────────────────────────────────
        val nature = (personality % 25).toInt()

        val shinySrc = (otId xor personality xor (personality ushr 16) xor (otId ushr 16))
        val isShiny = shinySrc < 8

        // ── Base stats from ROM ───────────────────────────────────────────────
        val baseStats = baseStatsReader?.invoke(speciesId)

        val baseHp     = baseStats?.get(DataHelper.BASE_STATS_HP)?.toInt()?.and(0xFF) ?: 0
        val baseAtk    = baseStats?.get(DataHelper.BASE_STATS_ATK)?.toInt()?.and(0xFF) ?: 0
        val baseDef    = baseStats?.get(DataHelper.BASE_STATS_DEF)?.toInt()?.and(0xFF) ?: 0
        val baseSpd    = baseStats?.get(DataHelper.BASE_STATS_SPD)?.toInt()?.and(0xFF) ?: 0
        val baseSpAtk  = baseStats?.get(DataHelper.BASE_STATS_SP_ATK)?.toInt()?.and(0xFF) ?: 0
        val baseSpDef  = baseStats?.get(DataHelper.BASE_STATS_SP_DEF)?.toInt()?.and(0xFF) ?: 0
        val type1      = baseStats?.get(DataHelper.BASE_STATS_TYPE1)?.toInt()?.and(0xFF) ?: 0
        val type2      = baseStats?.get(DataHelper.BASE_STATS_TYPE2)?.toInt()?.and(0xFF) ?: type1
        val genderRatio = baseStats?.get(DataHelper.BASE_STATS_GENDER_RATIO)?.toInt()?.and(0xFF) ?: 0xFF
        val expGroup   = baseStats?.get(DataHelper.BASE_STATS_EXP_GROUP)?.toInt()?.and(0xFF) ?: 0
        val ability1Id = baseStats?.get(DataHelper.BASE_STATS_ABILITY1)?.toInt()?.and(0xFF) ?: 0
        val ability2Id = baseStats?.get(DataHelper.BASE_STATS_ABILITY2)?.toInt()?.and(0xFF) ?: 0

        val gender: Gender = when {
            genderRatio == 0xFF -> Gender.NONE
            genderRatio == 0xFE -> Gender.FEMALE
            genderRatio == 0x00 -> Gender.MALE
            (personality and 0xFF) < genderRatio -> Gender.FEMALE
            else -> Gender.MALE
        }

        val moves = listOfNotNull(
            moveOrNull(move1Id, pp1, moveTable),
            moveOrNull(move2Id, pp2, moveTable),
            moveOrNull(move3Id, pp3, moveTable),
            moveOrNull(move4Id, pp4, moveTable),
        )

        return PokemonData(
            slot             = slot,
            speciesId        = speciesId,
            speciesName      = nameTable(speciesId),
            nickname         = nickname,
            level            = level,
            currentHp        = currentHp,
            maxHp            = maxHp,
            type1            = type1,
            type2            = type2,
            attack           = attack,
            defense          = defense,
            speed            = speed,
            spAtk            = spAtk,
            spDef            = spDef,
            moves            = moves,
            heldItemId       = heldItemId,
            experience       = experience,
            statusCondition  = statusCondition,
            nature           = nature,
            abilityIndex     = abilityIndex,
            ability1Id       = ability1Id,
            ability2Id       = ability2Id,
            baseHp           = baseHp,
            baseAtk          = baseAtk,
            baseDef          = baseDef,
            baseSpd          = baseSpd,
            baseSpAtk        = baseSpAtk,
            baseSpDef        = baseSpDef,
            expGroup         = expGroup,
            gender           = gender,
            isShiny          = isShiny,
            hasPokerus       = hasPokerus,
            ivHp             = ivHp,
            ivAtk            = ivAtk,
            ivDef            = ivDef,
            ivSpe            = ivSpe,
            ivSpA            = ivSpA,
            ivSpD            = ivSpD,
            evHp             = evHp,
            evAtk            = evAtk,
            evDef            = evDef,
            evSpe            = evSpe,
            evSpA            = evSpA,
            evSpD            = evSpD,
            friendship       = friendship,
            hiddenPowerType  = hiddenPowerType,
        )
    }

    private fun moveOrNull(moveId: Int, pp: Int, moveTable: (Int) -> String): MoveData? {
        if (moveId == 0) return null
        val stats = MoveStatsTable.get(moveId)
        return MoveData(
            moveId   = moveId,
            moveName = moveTable(moveId),
            pp       = pp,
            maxPp    = pp,
            power    = stats.power,
            accuracy = stats.accuracy,
            moveType = stats.type,
        )
    }

    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
        ((this[offset + 1].toLong() and 0xFF) shl 8) or
        ((this[offset + 2].toLong() and 0xFF) shl 16) or
        ((this[offset + 3].toLong() and 0xFF) shl 24)
}
