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

    private var curRow  = 0
    private var curCol  = 0
    private var curPage = 0

    /**
     * Navigates the naming screen to type [name], then presses OK.
     * Must be called from a coroutine. Sends key events on the calling thread.
     */
    suspend fun replay(name: String) {
        curRow = 0; curCol = 0; curPage = 0
        for (ch in name) {
            val found = NamingScreenData.findChar(ch) ?: continue
            val (targetPage, targetRow, targetCol) = found
            switchToPage(targetPage)
            navigateTo(targetRow, targetCol)
            pressKey(GBAKeys.GBA_KEY_A.key)
        }
        // Navigate to OK and confirm
        switchToPage(0)  // OK is same position on all pages; stay on current page
        navigateTo(NamingScreenData.OK_ROW, NamingScreenData.OK_COL)
        pressKey(GBAKeys.GBA_KEY_A.key)
    }

    private suspend fun switchToPage(targetPage: Int) {
        while (curPage != targetPage) {
            navigateTo(NamingScreenData.PAGE_TOGGLE_ROW, NamingScreenData.PAGE_TOGGLE_COL)
            pressKey(GBAKeys.GBA_KEY_A.key)
            curPage = (curPage + 1) % NamingScreenData.PAGES.size
            // Cursor stays at the toggle position after pressing it
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
