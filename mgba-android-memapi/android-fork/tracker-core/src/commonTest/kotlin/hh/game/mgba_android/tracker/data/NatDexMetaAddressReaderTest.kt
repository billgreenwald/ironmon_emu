package hh.game.mgba_android.tracker.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates NatDex 1.2.x address handling: detection of the ROM meta-table era and the field-by-field
 * mapping from meta-offsets to [GameAddresses]. Values mirror upstream NatDexExtension.lua v1.2.1.
 */
class NatDexMetaAddressReaderTest {

    /** Fake memory: returns `len` little-endian bytes of the stored value, or null if unmapped. */
    private fun reader(vals: Map<Long, Long>): (Long, Int) -> ByteArray? = { addr, len ->
        vals[addr]?.let { v -> ByteArray(len) { i -> ((v shr (8 * i)) and 0xFF).toByte() } }
    }

    // A complete, valid 1.2.x-style meta-table (offsets from updateGameSettings v1.2.1).
    private val metaTable: Map<Long, Long> = mapOf(
        0x08000150L to 0xEE0L,        // gameFlagsOffset
        0x080001BCL to 0x0829A60CL,   // baseStatsTable (ROM)
        0x080001CCL to 0x0827E344L,   // gBattleMoves (ROM)
        0x080001E4L to 0x50L,         // bagPocket_Items_size (u8)
        0x080001E8L to 0x2EL,         // bagPocket_Berries_size (u8)
        0x0800020CL to 0x02022FE8L,   // battleTypeFlags
        0x08000218L to 0x02024068L,   // battlersCount
        0x0800021CL to 0x0202406AL,   // gBattlerPartyIndexes
        0x08000228L to 0x02024080L,   // battleMons (EWRAM)
        0x08000240L to 0x02024278L,   // gMoveResultFlags
        0x08000244L to 0x0202427CL,   // gHitMarker
        0x08000248L to 0x0202428EL,   // sideStatuses
        0x0800024CL to 0x02024294L,   // sideTimers
        0x0800025CL to 0x0202432EL,   // gBattleCommunication
        0x08000260L to 0x02024336L,   // battleOutcome
        0x08000264L to 0x020243C8L,   // battleWeather
        0x08000278L to 0x02024284L,   // partyCount
        0x0800027CL to 0x02024744L,   // partyBase (EWRAM)
        0x08000280L to 0x02024E4CL,   // enemyParty
        0x08000284L to 0x020369D0L,   // gMapHeader
        0x0800028CL to 0x02036CB4L,   // sSpecialFlags
        0x08000294L to 0x03004E60L,   // trainerBattleOpponent
        0x080002B4L to 0xAF2L,        // gameStatsOffset
        0x080002BCL to 0x560L,        // bagPocket_Items_offset
        0x080002CCL to 0x790L,        // bagPocket_Berries_offset
        0x080002D0L to 0xF20L,        // encryptionKeyOffset
        0x080002D8L to 0x03004C40L,   // battleResults
        0x080002E0L to 0x03004CBCL,   // saveBlock1Ptr (IWRAM)
        0x080002E4L to 0x03004CC0L,   // saveBlock2Ptr
        0x08000308L to 0x0829B5DCL,   // experienceTables
        0x0800030CL to 0x08349750L,   // levelUpLearnsets
    )

    @Test
    fun readsAllFieldsFromMetaTable() {
        val a = NatDexMetaAddressReader.read(reader(metaTable))
        assertNotNull(a)
        assertEquals(0x02024284L, a.partyCount)
        assertEquals(0x02024744L, a.partyBase)
        assertEquals(0x02024E4CL, a.enemyParty)
        assertEquals(0x0829A60CL, a.baseStatsTable)
        assertEquals(0x08349750L, a.levelUpLearnsets)
        assertEquals(0x0829B5DCL, a.experienceTables)
        assertEquals(0x0827E344L, a.gBattleMoves)
        assertEquals(0x02024080L, a.battleMons)
        assertEquals(0x03004CBCL, a.saveBlock1Ptr)
        assertEquals(0x03004CC0L, a.saveBlock2Ptr)
        assertEquals(0xAF2, a.gameStatsOffset)
        assertEquals(0xEE0, a.gameFlagsOffset)
        assertEquals(0xF20, a.encryptionKeyOffset)
        assertEquals(0x560, a.bagPocket_Items_offset)
        assertEquals(0x50, a.bagPocket_Items_size)
        assertEquals(0x2E, a.bagPocket_Berries_size)
        assertTrue(a.saveBlock1IsPointer)
        assertTrue(a.hasFairy)
    }

    @Test
    fun readReturnsNullWhenTableUnreadable() {
        // Core not ready — every read returns null.
        assertNull(NatDexMetaAddressReader.read(reader(emptyMap())))
    }

    @Test
    fun validatesMetaTablePresence() {
        assertTrue(NatDexMetaAddressReader.isValidMetaTable(reader(metaTable)))
        assertTrue(!NatDexMetaAddressReader.isValidMetaTable(reader(emptyMap())))
    }

    @Test
    fun rejectsMetaTableWithWrongRegions() {
        // baseStatsTable pointing outside ROM → not a valid table (guards vanilla false-positives).
        val bad = metaTable + (0x080001BCL to 0x02020000L)
        assertTrue(!NatDexMetaAddressReader.isValidMetaTable(reader(bad)))
    }

    @Test
    fun detectsV12EraFromMetaTable() {
        // 1.2.x dex count (1258) is not 1210, so era must come from the meta-table.
        val vals = metaTable + (GameSettings.NATDEX_MON_COUNT_ADDR to 1258L)
        assertEquals(NatDexEra.V12, GameSettings.detectNatDexEra(reader(vals)))
    }

    @Test
    fun detectsV11EraFromLegacyCount() {
        val vals = mapOf(GameSettings.NATDEX_MON_COUNT_ADDR to 1210L)
        assertEquals(NatDexEra.V11, GameSettings.detectNatDexEra(reader(vals)))
    }

    @Test
    fun detectsNoneForNonNatDex() {
        // Not the legacy count and no valid meta-table.
        val vals = mapOf(GameSettings.NATDEX_MON_COUNT_ADDR to 386L)
        assertEquals(NatDexEra.NONE, GameSettings.detectNatDexEra(reader(vals)))
    }
}
