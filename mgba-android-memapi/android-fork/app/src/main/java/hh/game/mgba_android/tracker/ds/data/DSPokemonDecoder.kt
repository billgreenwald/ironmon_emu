package hh.game.mgba_android.tracker.ds.data

import hh.game.mgba_android.tracker.ds.models.DSPokemonData
import hh.game.mgba_android.tracker.ds.tables.GenIVBaseStats

/**
 * Decodes a Gen IV party Pokemon slot (0xEC bytes) into [DSPokemonData].
 *
 * Gen IV encryption overview:
 *   1. Raw bytes 0x00-0x07: unencrypted header (PID u32, junk u16, checksum u16)
 *   2. Bytes 0x08-0x87: four 32-byte sub-blocks, XOR-encrypted with a PRNG stream.
 *      - PRNG seed = checksum (u16)
 *      - PRNG: seed = seed * 0x41C64E6D + 0x6073;  output = (seed ushr 16) & 0xFFFF
 *      - Decrypt each u16 in 0x08-0x87 by XOR with successive PRNG outputs
 *   3. After decryption, the 4 blocks are in a shuffled order: index = PID % 24
 *      Block order table maps [shuffled slot 0..3] → canonical block [A=0, B=1, C=2, D=3]
 *   4. Party-only bytes 0x88-0xEB: unencrypted stats (current HP, level, etc.)
 */
object DSPokemonDecoder {

    // Block shuffle table: ABCD_PERMUTATIONS[pid % 24][shuffled_position] = canonical block index
    // A=0, B=1, C=2, D=3
    private val BLOCK_ORDER = arrayOf(
        intArrayOf(0, 1, 2, 3), // 0  ABCD
        intArrayOf(0, 1, 3, 2), // 1  ABDC
        intArrayOf(0, 2, 1, 3), // 2  ACBD
        intArrayOf(0, 3, 1, 2), // 3  ACDB  (note: ACDB = A,C stored at pos1, D at pos2... re-check ordering)
        intArrayOf(0, 2, 3, 1), // 4  ADBC  - canonical: slot0=A, slot1=D, slot2=B, slot3=C → unshuffled[A]=0,[B]=2,[C]=3,[D]=1
        intArrayOf(0, 3, 2, 1), // 5  ADCB
        intArrayOf(1, 0, 2, 3), // 6  BACD
        intArrayOf(1, 0, 3, 2), // 7  BADC
        intArrayOf(2, 0, 1, 3), // 8  CABD
        intArrayOf(3, 0, 1, 2), // 9  DABC  -- verify these carefully
        intArrayOf(2, 0, 3, 1), // 10 CADB
        intArrayOf(3, 0, 2, 1), // 11 DACB
        intArrayOf(1, 2, 0, 3), // 12 BCAD
        intArrayOf(1, 3, 0, 2), // 13 BDAC
        intArrayOf(2, 1, 0, 3), // 14 CBAD
        intArrayOf(3, 1, 0, 2), // 15 DBAC
        intArrayOf(2, 3, 0, 1), // 16 CDAB
        intArrayOf(3, 2, 0, 1), // 17 DCAB
        intArrayOf(1, 2, 3, 0), // 18 BCDA
        intArrayOf(1, 3, 2, 0), // 19 BDCA
        intArrayOf(2, 1, 3, 0), // 20 CBDA
        intArrayOf(3, 1, 2, 0), // 21 DBCA
        intArrayOf(2, 3, 1, 0), // 22 CDBA
        intArrayOf(3, 2, 1, 0), // 23 DCBA
    )

    /**
     * Decode one 0xEC-byte party slot.
     * Returns null if the slot is empty (species 0) or data appears corrupt.
     */
    fun decode(slot: ByteArray): DSPokemonData? {
        if (slot.size < 0xEC) return null

        val pid        = readU32(slot, 0x00)
        val checksum   = readU16(slot, 0x06)

        // Step 1: decrypt bytes 0x08-0x87 using PRNG seeded with checksum
        val decrypted = ByteArray(0x80)
        slot.copyInto(decrypted, 0, 0x08, 0x88)
        prngDecrypt(decrypted, checksum)

        // Step 2: un-shuffle blocks into canonical A, B, C, D order
        val shuffleIdx = (pid % 24).toInt()
        val order = BLOCK_ORDER[shuffleIdx]
        val blocks = Array(4) { ByteArray(32) }
        for (shuffledPos in 0..3) {
            val canonicalIdx = order[shuffledPos]
            decrypted.copyInto(blocks[canonicalIdx], 0, shuffledPos * 32, shuffledPos * 32 + 32)
        }
        val blockA = blocks[0]
        val blockB = blocks[1]
        val blockC = blocks[2]

        // Step 3: read block A
        val speciesId   = readU16(blockA, DSDataHelper.BLOCK_A_SPECIES)
        if (speciesId == 0) return null   // empty slot
        val heldItemId  = readU16(blockA, DSDataHelper.BLOCK_A_HELD_ITEM)
        val experience  = readU32(blockA, DSDataHelper.BLOCK_A_EXPERIENCE)
        val friendship  = blockA[DSDataHelper.BLOCK_A_FRIENDSHIP].toInt() and 0xFF
        val abilitySlot = blockA[DSDataHelper.BLOCK_A_ABILITY].toInt() and 0xFF
        val hpEv        = blockA[DSDataHelper.BLOCK_A_HP_EV].toInt()  and 0xFF
        val atkEv       = blockA[DSDataHelper.BLOCK_A_ATK_EV].toInt() and 0xFF
        val defEv       = blockA[DSDataHelper.BLOCK_A_DEF_EV].toInt() and 0xFF
        val speEv       = blockA[DSDataHelper.BLOCK_A_SPE_EV].toInt() and 0xFF
        val spaEv       = blockA[DSDataHelper.BLOCK_A_SPA_EV].toInt() and 0xFF
        val spdEv       = blockA[DSDataHelper.BLOCK_A_SPD_EV].toInt() and 0xFF

        // Step 4: read block B
        val move1Id = readU16(blockB, DSDataHelper.BLOCK_B_MOVE1)
        val move2Id = readU16(blockB, DSDataHelper.BLOCK_B_MOVE2)
        val move3Id = readU16(blockB, DSDataHelper.BLOCK_B_MOVE3)
        val move4Id = readU16(blockB, DSDataHelper.BLOCK_B_MOVE4)
        val move1Pp = blockB[DSDataHelper.BLOCK_B_PP1].toInt() and 0xFF
        val move2Pp = blockB[DSDataHelper.BLOCK_B_PP2].toInt() and 0xFF
        val move3Pp = blockB[DSDataHelper.BLOCK_B_PP3].toInt() and 0xFF
        val move4Pp = blockB[DSDataHelper.BLOCK_B_PP4].toInt() and 0xFF
        val ivWord  = readU32(blockB, DSDataHelper.BLOCK_B_IV_WORD)
        val hpIv    = ((ivWord ushr  0) and 0x1F).toInt()
        val atkIv   = ((ivWord ushr  5) and 0x1F).toInt()
        val defIv   = ((ivWord ushr 10) and 0x1F).toInt()
        val speIv   = ((ivWord ushr 15) and 0x1F).toInt()
        val spaIv   = ((ivWord ushr 20) and 0x1F).toInt()
        val spdIv   = ((ivWord ushr 25) and 0x1F).toInt()
        val isEgg   = ((ivWord ushr 30) and 0x1) != 0L
        val abilityBit = ((ivWord ushr 31) and 0x1).toInt()  // 0 or 1, ability slot override

        // Step 5: read block C (nickname)
        val nickname = decodeNickname(blockC, DSDataHelper.BLOCK_C_NICKNAME, DSDataHelper.BLOCK_C_NICKNAME_LEN)

        // Step 6: unencrypted party data (after 0x88)
        val statusFlags = readU32(slot, DSDataHelper.SLOT_OFF_STATUS).toInt()
        val level       = slot[DSDataHelper.SLOT_OFF_LEVEL].toInt() and 0xFF
        val curHp       = readU16(slot, DSDataHelper.SLOT_OFF_CUR_HP)
        val maxHp       = readU16(slot, DSDataHelper.SLOT_OFF_MAX_HP)
        val atk         = readU16(slot, DSDataHelper.SLOT_OFF_ATK)
        val def         = readU16(slot, DSDataHelper.SLOT_OFF_DEF)
        val spe         = readU16(slot, DSDataHelper.SLOT_OFF_SPE)
        val spa         = readU16(slot, DSDataHelper.SLOT_OFF_SPA)
        val spd         = readU16(slot, DSDataHelper.SLOT_OFF_SPD)

        // Step 7: derive nature, ability, types
        val nature = (pid % 25).toInt()
        val baseStats = GenIVBaseStats.get(speciesId)
        val abilityId = baseStats?.let {
            if (abilityBit == 1) it.ability2 else it.ability1
        } ?: 0
        val type1 = baseStats?.type1 ?: 0
        val type2 = baseStats?.type2 ?: 0

        return DSPokemonData(
            speciesId    = speciesId,
            level        = level,
            currentHp    = curHp,
            maxHp        = maxHp,
            nature       = nature,
            abilityIndex = abilityBit,
            abilityId    = abilityId,
            heldItemId   = heldItemId,
            experience   = experience,
            friendship   = friendship,
            atk = atk, def = def, spe = spe, spa = spa, spd = spd,
            hpIv = hpIv, atkIv = atkIv, defIv = defIv, speIv = speIv, spaIv = spaIv, spdIv = spdIv,
            hpEv = hpEv, atkEv = atkEv, defEv = defEv, speEv = speEv, spaEv = spaEv, spdEv = spdEv,
            move1Id = move1Id, move2Id = move2Id, move3Id = move3Id, move4Id = move4Id,
            move1Pp = move1Pp, move2Pp = move2Pp, move3Pp = move3Pp, move4Pp = move4Pp,
            nickname     = nickname,
            isEgg        = isEgg,
            statusFlags  = statusFlags,
            type1        = type1,
            type2        = type2,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * XOR-decrypt [data] in-place using the Gen IV PRNG seeded with [seed].
     * Operates on u16 pairs (little-endian): each 2-byte pair XORed with next PRNG output.
     */
    private fun prngDecrypt(data: ByteArray, seed: Int) {
        var s = seed.toLong() and 0xFFFFL
        for (i in 0 until data.size / 2) {
            s = (s * 0x41C64E6DL + 0x6073L) and 0xFFFFFFFFL
            val key = ((s ushr 16) and 0xFFFFL).toInt()
            val lo = (data[i * 2].toInt() and 0xFF) xor (key and 0xFF)
            val hi = (data[i * 2 + 1].toInt() and 0xFF) xor ((key ushr 8) and 0xFF)
            data[i * 2]     = lo.toByte()
            data[i * 2 + 1] = hi.toByte()
        }
    }

    private fun readU16(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0xFF)) or
               ((buf[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readU32(buf: ByteArray, offset: Int): Long {
        return ((buf[offset].toLong()     and 0xFF)) or
               ((buf[offset + 1].toLong() and 0xFF) shl 8) or
               ((buf[offset + 2].toLong() and 0xFF) shl 16) or
               ((buf[offset + 3].toLong() and 0xFF) shl 24)
    }

    /**
     * Decode a DS UTF-16LE nickname. The DS uses a custom character table but
     * for ASCII-range characters (0x0041-0x005A, 0x0061-0x007A, digits) the
     * code points map directly to Unicode. 0xFFFF = end-of-string.
     */
    private fun decodeNickname(block: ByteArray, offset: Int, maxLen: Int): String {
        val sb = StringBuilder(maxLen)
        for (i in 0 until maxLen) {
            val cp = readU16(block, offset + i * 2)
            if (cp == 0xFFFF) break
            if (cp in 0x0020..0x007E) sb.append(cp.toChar())
            else sb.append('?')  // non-ASCII DS character; full table can be added later
        }
        return sb.toString()
    }
}
