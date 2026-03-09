package com.ironmon.tracker

import com.ironmon.tracker.data.PokemonDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for PokemonDecoder — no emulator required.
 *
 * Tests cover all 24 personality%24 orderings to ensure substructure
 * positions are calculated correctly for every permutation.
 *
 * Test data uses a simplified hand-crafted 100-byte array with known values.
 */
class PokemonDecoderTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val dummyNameTable: (Int) -> String = { id -> "Species#$id" }
    private val dummyMoveTable: (Int) -> String = { id -> "Move#$id"    }

    /**
     * Build a 100-byte raw party Pokémon block with controlled values.
     *
     * @param personality Personality value (determines substructure order)
     * @param otId        OT ID (used in XOR key with personality)
     * @param speciesId   Species to put in Growth substructure slot 0
     * @param moveId1     First move ID to put in Attacks substructure slot 0
     * @param level       Level byte at offset 0x54
     * @param currentHp   Current HP u16 at 0x56
     * @param maxHp       Max HP u16 at 0x58
     */
    private fun buildRaw(
        personality: Long,
        otId: Long,
        speciesId: Int,
        moveId1: Int   = 33,   // Tackle
        level: Int     = 5,
        currentHp: Int = 20,
        maxHp: Int     = 20,
    ): ByteArray {
        val raw = ByteArray(100)

        // Write personality (u32 LE)
        raw[0x00] = (personality and 0xFF).toByte()
        raw[0x01] = ((personality shr 8) and 0xFF).toByte()
        raw[0x02] = ((personality shr 16) and 0xFF).toByte()
        raw[0x03] = ((personality shr 24) and 0xFF).toByte()

        // Write OT ID (u32 LE)
        raw[0x04] = (otId and 0xFF).toByte()
        raw[0x05] = ((otId shr 8) and 0xFF).toByte()
        raw[0x06] = ((otId shr 16) and 0xFF).toByte()
        raw[0x07] = ((otId shr 24) and 0xFF).toByte()

        // Determine substructure layout
        val orderIndex   = (personality % 24).toInt()
        val xorKey       = personality xor otId
        val growthSlot   = SUBSTRUCTURE_ORDER[orderIndex][0]
        val attacksSlot  = SUBSTRUCTURE_ORDER[orderIndex][1]

        // Build plaintext 48-byte substructure block
        val plain = ByteArray(48)

        // Growth block: species at slot_offset + 0x00
        val gBase = growthSlot * 12
        plain[gBase + 0] = (speciesId and 0xFF).toByte()
        plain[gBase + 1] = ((speciesId shr 8) and 0xFF).toByte()

        // Attacks block: move1 at slot_offset + 0x00, PP at + 0x08
        val aBase = attacksSlot * 12
        plain[aBase + 0] = (moveId1 and 0xFF).toByte()
        plain[aBase + 1] = ((moveId1 shr 8) and 0xFF).toByte()
        plain[aBase + 8] = 35.toByte()  // PP = 35 (Tackle's max PP)

        // XOR-encrypt: 12 words of 4 bytes each
        for (i in 0 until 12) {
            val wordOffset = i * 4
            val word = ((plain[wordOffset].toLong() and 0xFF) or
                       ((plain[wordOffset+1].toLong() and 0xFF) shl 8) or
                       ((plain[wordOffset+2].toLong() and 0xFF) shl 16) or
                       ((plain[wordOffset+3].toLong() and 0xFF) shl 24)) xor xorKey
            raw[0x20 + wordOffset + 0] = (word and 0xFF).toByte()
            raw[0x20 + wordOffset + 1] = ((word shr 8) and 0xFF).toByte()
            raw[0x20 + wordOffset + 2] = ((word shr 16) and 0xFF).toByte()
            raw[0x20 + wordOffset + 3] = ((word shr 24) and 0xFF).toByte()
        }

        // Unencrypted stats
        raw[0x54] = level.toByte()
        raw[0x56] = (currentHp and 0xFF).toByte()
        raw[0x57] = ((currentHp shr 8) and 0xFF).toByte()
        raw[0x58] = (maxHp and 0xFF).toByte()
        raw[0x59] = ((maxHp shr 8) and 0xFF).toByte()

        return raw
    }

    // From PokemonDecoder (duplicated for test self-containment)
    private val SUBSTRUCTURE_ORDER: Array<IntArray> = arrayOf(
        intArrayOf(0, 1, 2, 3), intArrayOf(0, 1, 3, 2), intArrayOf(0, 2, 1, 3),
        intArrayOf(0, 3, 1, 2), intArrayOf(0, 2, 3, 1), intArrayOf(0, 3, 2, 1),
        intArrayOf(1, 0, 2, 3), intArrayOf(1, 0, 3, 2), intArrayOf(2, 0, 1, 3),
        intArrayOf(3, 0, 1, 2), intArrayOf(2, 0, 3, 1), intArrayOf(3, 0, 2, 1),
        intArrayOf(1, 2, 0, 3), intArrayOf(1, 3, 0, 2), intArrayOf(2, 1, 0, 3),
        intArrayOf(3, 1, 0, 2), intArrayOf(2, 3, 0, 1), intArrayOf(3, 2, 0, 1),
        intArrayOf(1, 2, 3, 0), intArrayOf(1, 3, 2, 0), intArrayOf(2, 1, 3, 0),
        intArrayOf(3, 1, 2, 0), intArrayOf(2, 3, 1, 0), intArrayOf(3, 2, 1, 0),
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    /** Test all 24 substructure orderings with personality values 0–23 */
    @Test
    fun `all 24 substructure orderings decode species correctly`() {
        for (pv in 0L until 24L) {
            val expectedSpecies = 25  // Pikachu
            val raw = buildRaw(personality = pv, otId = 0L, speciesId = expectedSpecies)
            val result = PokemonDecoder.decode(
                slot = 0, raw = raw,
                nameTable = dummyNameTable, moveTable = dummyMoveTable,
            )
            assertNotNull("PV=$pv: expected non-null result", result)
            assertEquals("PV=$pv: species mismatch", expectedSpecies, result!!.speciesId)
        }
    }

    @Test
    fun `empty slot (species 0) returns null`() {
        val raw = buildRaw(personality = 0L, otId = 0L, speciesId = 0)
        val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
        assertNull("Expected null for empty slot", result)
    }

    @Test
    fun `level is read from unencrypted header`() {
        val raw = buildRaw(personality = 7L, otId = 12345L, speciesId = 6, level = 42)
        val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
        assertNotNull(result)
        assertEquals(42, result!!.level)
    }

    @Test
    fun `HP values are read correctly`() {
        val raw = buildRaw(
            personality = 3L, otId = 9999L, speciesId = 150,
            currentHp = 187, maxHp = 354,
        )
        val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
        assertNotNull(result)
        assertEquals(187, result!!.currentHp)
        assertEquals(354, result.maxHp)
    }

    @Test
    fun `move ID decoded for each personality ordering`() {
        for (pv in 0L until 24L) {
            val raw = buildRaw(
                personality = pv, otId = 0x1337L,
                speciesId = 1, moveId1 = 87,  // Thunder
            )
            val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
            assertNotNull("PV=$pv: result was null", result)
            val moves = result!!.moves
            assert(moves.isNotEmpty()) { "PV=$pv: expected at least 1 move" }
            assertEquals("PV=$pv: move ID mismatch", 87, moves[0].moveId)
        }
    }

    @Test
    fun `XOR key uses personality XOR otId`() {
        // Choose a personality and OT ID where personality XOR otId != 0
        val personality = 0xDEADBEEFL
        val otId        = 0xCAFEBABEL
        val raw = buildRaw(personality = personality, otId = otId, speciesId = 130)  // Gyarados
        val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
        assertNotNull(result)
        assertEquals(130, result!!.speciesId)
    }

    @Test
    fun `slot index is preserved`() {
        val raw = buildRaw(personality = 0L, otId = 0L, speciesId = 25)
        val result = PokemonDecoder.decode(3, raw, dummyNameTable, dummyMoveTable)
        assertNotNull(result)
        assertEquals(3, result!!.slot)
    }

    @Test
    fun `large HP values (u16) decoded without sign extension`() {
        val raw = buildRaw(
            personality = 0L, otId = 0L, speciesId = 113,  // Chansey
            currentHp = 600, maxHp = 700,
        )
        val result = PokemonDecoder.decode(0, raw, dummyNameTable, dummyMoveTable)
        assertNotNull(result)
        assertEquals(600, result!!.currentHp)
        assertEquals(700, result.maxHp)
    }
}
