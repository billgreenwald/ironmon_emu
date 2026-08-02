package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.GameVersion
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validates the common (JVM-Charset-free) ROM game-code decode added during the KMP move.
 */
class GameSettingsTest {

    private fun codeBytes(code: String): ByteArray = code.map { it.code.toByte() }.toByteArray()

    @Test
    fun detectsFireRed() =
        assertEquals(GameVersion.FIRE_RED, GameSettings.detectGame(codeBytes("BPRE")))

    @Test
    fun detectsLeafGreen() =
        assertEquals(GameVersion.LEAF_GREEN, GameSettings.detectGame(codeBytes("BPGE")))

    @Test
    fun detectsEmerald() =
        assertEquals(GameVersion.EMERALD, GameSettings.detectGame(codeBytes("BPEE")))

    @Test
    fun unknownForGarbage() =
        assertEquals(GameVersion.UNKNOWN, GameSettings.detectGame(byteArrayOf(0, 0, 0, 0)))

    @Test
    fun unknownForShortInput() =
        assertEquals(GameVersion.UNKNOWN, GameSettings.detectGame(byteArrayOf(0x42)))
}
