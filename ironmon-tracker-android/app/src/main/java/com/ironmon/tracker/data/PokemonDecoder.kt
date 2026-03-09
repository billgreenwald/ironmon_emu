package com.ironmon.tracker.data

import com.ironmon.tracker.data.models.MoveData
import com.ironmon.tracker.data.models.PokemonData

/**
 * Decodes a raw 100-byte Gen III party Pokémon struct.
 *
 * Gen III Pokémon data uses an XOR cipher and a personality-value-dependent
 * substructure ordering (24 possible orderings — one per permutation of the
 * 4 substructure types G/A/E/M).
 *
 * Reference: https://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9mon_data_structure_(Generation_III)
 */
object PokemonDecoder {

    /**
     * The 24 possible substructure orderings based on personality % 24.
     * Each entry is [G, A, E, M] → index within the 4 slots of the encrypted block.
     * Index meaning: 0=Growth, 1=Attacks, 2=EVs/Condition, 3=Misc
     */
    private val SUBSTRUCTURE_ORDER: Array<IntArray> = arrayOf(
        // order → [G_pos, A_pos, E_pos, M_pos]
        intArrayOf(0, 1, 2, 3), // 0:  GAEM
        intArrayOf(0, 1, 3, 2), // 1:  GAME
        intArrayOf(0, 2, 1, 3), // 2:  GEAM
        intArrayOf(0, 3, 1, 2), // 3:  GEMA
        intArrayOf(0, 2, 3, 1), // 4:  GMAE — wait, plan maps to GAEM permutations
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

    /**
     * Decode a single Pokémon from 100 raw bytes.
     *
     * @param slot      Party slot index (0–5)
     * @param raw       100-byte array read from memory starting at partyBase + slot*100
     * @param nameTable Function to resolve species ID → name string
     * @param moveTable Function to resolve move ID → move name string
     * @return Decoded PokemonData, or null if the slot is empty (species == 0)
     */
    fun decode(
        slot: Int,
        raw: ByteArray,
        nameTable: (Int) -> String,
        moveTable: (Int) -> String,
    ): PokemonData? {
        require(raw.size >= 100) { "Raw bytes must be at least 100 bytes, got ${raw.size}" }

        // ── 1. Read personality and OT ID (unencrypted header) ──────────────
        val personality = raw.u32(DataHelper.OFF_PERSONALITY)
        val otId        = raw.u32(DataHelper.OFF_OT_ID)

        // ── 2. Decrypt the 48-byte substructure block ────────────────────────
        val xorKey = personality xor otId
        val decrypted = ByteArray(48)
        for (i in 0 until 12) {
            // Each 32-bit word is XOR'd with the full key
            val word = raw.u32(DataHelper.OFF_ENCRYPTED + i * 4) xor xorKey
            decrypted[i * 4 + 0] = (word and 0xFF).toByte()
            decrypted[i * 4 + 1] = ((word shr 8) and 0xFF).toByte()
            decrypted[i * 4 + 2] = ((word shr 16) and 0xFF).toByte()
            decrypted[i * 4 + 3] = ((word shr 24) and 0xFF).toByte()
        }

        // ── 3. Determine substructure positions ──────────────────────────────
        val orderIndex = (personality % 24).toInt()
        val order      = SUBSTRUCTURE_ORDER[orderIndex]
        // order[i] = position of substructure i (G=0, A=1, E=2, M=3)
        // So Growth block starts at decrypted[order[0] * 12]
        val growthOffset  = order[0] * 12
        val attacksOffset = order[1] * 12

        // ── 4. Extract Growth substructure ───────────────────────────────────
        val speciesId  = decrypted.u16(growthOffset + DataHelper.GROWTH_SPECIES)
        val heldItemId = decrypted.u16(growthOffset + DataHelper.GROWTH_ITEM)
        val experience = decrypted.u32le(growthOffset + DataHelper.GROWTH_EXP).toInt()

        // Empty slot check — species 0 means no Pokémon
        if (speciesId == 0) return null

        // ── 5. Extract Attacks substructure ─────────────────────────────────
        val move1Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE1)
        val move2Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE2)
        val move3Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE3)
        val move4Id = decrypted.u16(attacksOffset + DataHelper.ATK_MOVE4)
        val pp1     = decrypted[attacksOffset + DataHelper.ATK_PP1].toInt() and 0xFF
        val pp2     = decrypted[attacksOffset + DataHelper.ATK_PP2].toInt() and 0xFF
        val pp3     = decrypted[attacksOffset + DataHelper.ATK_PP3].toInt() and 0xFF
        val pp4     = decrypted[attacksOffset + DataHelper.ATK_PP4].toInt() and 0xFF

        // ── 6. Read unencrypted in-battle stats ──────────────────────────────
        val level     = raw[DataHelper.OFF_LEVEL].toInt() and 0xFF
        val currentHp = raw.u16(DataHelper.OFF_CURRENT_HP)
        val maxHp     = raw.u16(DataHelper.OFF_MAX_HP)
        val attack    = raw.u16(DataHelper.OFF_ATTACK)
        val defense   = raw.u16(DataHelper.OFF_DEFENSE)
        val speed     = raw.u16(DataHelper.OFF_SPEED)
        val spAtk     = raw.u16(DataHelper.OFF_SP_ATK)
        val spDef     = raw.u16(DataHelper.OFF_SP_DEF)

        // ── 7. Assemble moves ────────────────────────────────────────────────
        val moves = listOfNotNull(
            moveOrNull(move1Id, pp1, moveTable),
            moveOrNull(move2Id, pp2, moveTable),
            moveOrNull(move3Id, pp3, moveTable),
            moveOrNull(move4Id, pp4, moveTable),
        )

        return PokemonData(
            slot        = slot,
            speciesId   = speciesId,
            speciesName = nameTable(speciesId),
            level       = level,
            currentHp   = currentHp,
            maxHp       = maxHp,
            type1       = 0,   // loaded separately from base stats ROM table
            type2       = 0,
            attack      = attack,
            defense     = defense,
            speed       = speed,
            spAtk       = spAtk,
            spDef       = spDef,
            moves       = moves,
            heldItemId  = heldItemId,
            experience  = experience,
        )
    }

    private fun moveOrNull(moveId: Int, pp: Int, moveTable: (Int) -> String): MoveData? {
        if (moveId == 0) return null
        return MoveData(
            moveId   = moveId,
            moveName = moveTable(moveId),
            pp       = pp,
            maxPp    = pp,   // max PP requires base move data lookup; use current as estimate
        )
    }

    // ── ByteArray extension helpers ──────────────────────────────────────────

    /** Read little-endian u16 at offset */
    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8)

    /** Read little-endian u32 at offset, returning Long (avoids sign extension) */
    private fun ByteArray.u32(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
        ((this[offset + 1].toLong() and 0xFF) shl 8) or
        ((this[offset + 2].toLong() and 0xFF) shl 16) or
        ((this[offset + 3].toLong() and 0xFF) shl 24)

    /** Read little-endian u32 at offset as Long */
    private fun ByteArray.u32le(offset: Int): Long = u32(offset)
}
