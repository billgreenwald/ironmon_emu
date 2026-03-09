package com.ironmon.tracker

import com.ironmon.tracker.data.GameSettings
import com.ironmon.tracker.data.models.GameVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSettingsTest {

    private fun code(s: String): ByteArray = s.toByteArray(Charsets.ISO_8859_1)

    @Test fun `detects Fire Red`()   = assertEquals(GameVersion.FIRE_RED,   GameSettings.detectGame(code("BPRE")))
    @Test fun `detects Leaf Green`() = assertEquals(GameVersion.LEAF_GREEN, GameSettings.detectGame(code("BPGE")))
    @Test fun `detects Ruby`()       = assertEquals(GameVersion.RUBY,       GameSettings.detectGame(code("AXVE")))
    @Test fun `detects Sapphire`()   = assertEquals(GameVersion.SAPPHIRE,   GameSettings.detectGame(code("AXPE")))
    @Test fun `detects Emerald`()    = assertEquals(GameVersion.EMERALD,    GameSettings.detectGame(code("BPEE")))

    @Test fun `detects Fire Red PAL variant`() =
        assertEquals(GameVersion.FIRE_RED, GameSettings.detectGame(code("BPRS")))

    @Test fun `returns UNKNOWN for garbage code`() =
        assertEquals(GameVersion.UNKNOWN, GameSettings.detectGame(code("XXXX")))

    @Test fun `returns UNKNOWN for empty input`() =
        assertEquals(GameVersion.UNKNOWN, GameSettings.detectGame(ByteArray(0)))

    @Test fun `returns UNKNOWN for short input`() =
        assertEquals(GameVersion.UNKNOWN, GameSettings.detectGame(byteArrayOf(0x42, 0x50)))
}
