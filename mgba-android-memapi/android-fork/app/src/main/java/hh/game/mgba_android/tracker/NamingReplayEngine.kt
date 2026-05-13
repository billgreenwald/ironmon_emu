package hh.game.mgba_android.tracker

import hh.game.mgba_android.tracker.data.NamingScreenData
import hh.game.mgba_android.utils.GBAKeys
import kotlinx.coroutines.delay
import org.libsdl.app.SDLUtils

/**
 * Replays D-pad + A button presses to navigate the Gen III naming screen and type a name.
 *
 * Assumes the in-game cursor is at (row=0, col=0) on the uppercase page when [replay] is called.
 * Characters not found in any page are silently skipped.
 */
object NamingReplayEngine {
    private const val PRESS_MS  = 80L  // key-down hold duration
    private const val RELEASE_MS = 40L // gap after key-up before next action

    // Set to true if the game auto-advances the cursor right after each A press
    var cursorAutoAdvances = false

    private var curRow  = 0
    private var curCol  = 0
    private var curPage = 0

    /**
     * Navigates the naming screen to type [name], then presses OK.
     * [setSpeed] and [restoreSpeed] are called before/after to ensure 1x emulation speed.
     * Must be called from a coroutine on the main thread.
     */
    suspend fun replay(name: String, setSpeed: () -> Unit, restoreSpeed: () -> Unit) {
        curRow = 0; curCol = 0; curPage = 0
        setSpeed()
        try { for (ch in name) {
            val found = NamingScreenData.findChar(ch) ?: continue
            val (targetPage, targetRow, targetCol) = found
            switchToPage(targetPage)
            navigateTo(targetRow, targetCol)
            pressKey(GBAKeys.GBA_KEY_A.key)
            if (cursorAutoAdvances && curCol < NamingScreenData.PAGES[curPage][curRow].size - 1) curCol++
        }
        // START + A confirms the name
        pressKey(GBAKeys.GBA_KEY_START.key)
        pressKey(GBAKeys.GBA_KEY_A.key)
        } finally { restoreSpeed() }
    }

    private suspend fun switchToPage(targetPage: Int) {
        while (curPage != targetPage) {
            pressKey(GBAKeys.GBA_KEY_SELECT.key)
            delay(500) // wait for page transition before navigating
            curPage = (curPage + 1) % NamingScreenData.PAGES.size
        }
    }

    private suspend fun navigateTo(row: Int, col: Int) {
        val dRow = row - curRow
        val dCol = col - curCol
        val rowKey = if (dRow > 0) GBAKeys.GBA_KEY_DOWN.key else GBAKeys.GBA_KEY_UP.key
        val colKey = if (dCol > 0) GBAKeys.GBA_KEY_RIGHT.key else GBAKeys.GBA_KEY_LEFT.key
        repeat(kotlin.math.abs(dRow)) { pressKey(rowKey) }
        repeat(kotlin.math.abs(dCol)) { pressKey(colKey) }
        curRow = row
        curCol = col
    }

    private suspend fun pressKey(keycode: Int) {
        SDLUtils.onNativeKeyDown(keycode)
        delay(PRESS_MS)
        SDLUtils.onNativeKeyUp(keycode)
        delay(RELEASE_MS)
    }
}
