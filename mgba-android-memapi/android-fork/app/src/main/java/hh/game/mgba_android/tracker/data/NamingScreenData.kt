package hh.game.mgba_android.tracker.data

/**
 * Keyboard layout for the Gen III Pokemon naming screen.
 * 7 columns x 4 letter rows. Page 0 = uppercase, Page 1 = lowercase.
 *
 * Row 0: A B C D E F .
 * Row 1: G H I J K L ,
 * Row 2: M N O P Q R S
 * Row 3: T U V W X Y Z
 *
 * Page switching: SELECT jumps cursor to toggle; A confirms.
 * Confirming name: START button.
 */
object NamingScreenData {
    // Sentinel chars for special keys -- control chars that never appear in Pokemon names
    const val KEY_PAGE_TOGGLE = '\u0001'
    const val KEY_DEL         = '\u0008'

    val UPPERCASE_PAGE: List<List<Char>> = listOf(
        listOf('A','B','C','D','E','F','.'),
        listOf('G','H','I','J','K','L',','),
        listOf('M','N','O','P','Q','R','S'),
        listOf('T','U','V','W','X','Y','Z'),
    )

    val LOWERCASE_PAGE: List<List<Char>> = listOf(
        listOf('a','b','c','d','e','f','.'),
        listOf('g','h','i','j','k','l',','),
        listOf('m','n','o','p','q','r','s'),
        listOf('t','u','v','w','x','y','z'),
    )

    // Page 2 — special characters (layout TBD; included so the cycle count is correct)
    val SPECIAL_PAGE: List<List<Char>> = emptyList()

    // Pages cycle: uppercase → lowercase → special → uppercase
    val PAGES: List<List<List<Char>>> = listOf(UPPERCASE_PAGE, LOWERCASE_PAGE, SPECIAL_PAGE)

    // After SELECT, cursor lands on the page toggle button at this position
    const val PAGE_TOGGLE_ROW = 4
    const val PAGE_TOGGLE_COL = 0

    /** Returns (pageIndex, row, col) for [ch], or null if not found. */
    fun findChar(ch: Char): Triple<Int, Int, Int>? {
        for ((pi, page) in PAGES.withIndex()) {
            for ((ri, row) in page.withIndex()) {
                val ci = row.indexOf(ch)
                if (ci >= 0) return Triple(pi, ri, ci)
            }
        }
        return null
    }
}
