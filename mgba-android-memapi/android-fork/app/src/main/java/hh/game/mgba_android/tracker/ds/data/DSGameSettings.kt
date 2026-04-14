package hh.game.mgba_android.tracker.ds.data

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.ds.models.DSGameVersion

/**
 * Detects which DS game is loaded by reading the cartridge game code from the
 * DS header mirror in ARM9 main RAM.
 *
 * The DS cartridge header is mirrored at 0x027FFE00.
 * The 4-byte game code starts at offset +0x0C (i.e. 0x027FFE0C).
 */
object DSGameSettings {

    fun detectVersion(): DSGameVersion {
        val bytes = MemoryBridge.readBytes(DSGameVersion.GAME_CODE_ADDRESS.toLong(), 4) ?: return DSGameVersion.UNKNOWN
        val code = String(bytes, Charsets.US_ASCII).trimEnd('\u0000')
        return DSGameVersion.fromGameCode(code)
    }
}
