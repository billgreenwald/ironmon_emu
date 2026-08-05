package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.data.NamingScreenData
import kotlin.math.abs

/**
 * Generates the GBA button sequence to type a name on the Gen III naming screen, ported from the
 * Android `NamingReplayEngine` navigation logic. The platform layer executes the returned keys with
 * its own press/release timing (Swift `EmulatorCore.setKeys`, Android SDL).
 *
 * Key codes are mGBA GBAKey bit positions (match iOS `GBAKeyMask` bit indices).
 * Assumes the in-game cursor starts at page 0 / row 0 / col 0.
 */
object NamingSequence {
    const val A = 0
    const val SELECT = 2
    const val START = 3
    const val RIGHT = 4
    const val LEFT = 5
    const val UP = 6
    const val DOWN = 7

    /** A single key press. [longDelay] marks page switches, which need a longer settle time. */
    data class Key(val code: Int, val longDelay: Boolean = false)

    fun keysFor(name: String): List<Key> {
        val out = mutableListOf<Key>()
        var page = 0
        var row = 0
        var col = 0
        for (ch in name) {
            val found = NamingScreenData.findChar(ch) ?: continue
            val (targetPage, targetRow, targetCol) = found
            while (page != targetPage) {
                out.add(Key(SELECT, longDelay = true))
                page = (page + 1) % NamingScreenData.PAGES.size
            }
            val dRow = targetRow - row
            repeat(abs(dRow)) { out.add(Key(if (dRow > 0) DOWN else UP)) }
            row = targetRow
            val dCol = targetCol - col
            repeat(abs(dCol)) { out.add(Key(if (dCol > 0) RIGHT else LEFT)) }
            col = targetCol
            out.add(Key(A))
        }
        // START + A confirms the name.
        out.add(Key(START))
        out.add(Key(A))
        return out
    }
}
