package hh.game.mgba_android.tracker.data

/**
 * Keyboard layout for the Gen III Pokemon naming screen (FR/LG and RSE share this layout).
 *
 * Grid is 9 columns x 4 rows per page. Row 3 is the control row.
 * KEY_PAGE_TOGGLE switches to the next page; KEY_DEL backspaces; KEY_OK confirms.
 *
 * NOTE: Verify these positions against the actual game before shipping.
 */
object NamingScreenData {
    // Sentinel chars for special keys -- control chars that never appear in Pokemon names
    const val KEY_PAGE_TOGGLE = '\u0001'
    const val KEY_DEL         = '\u0008'
    const val KEY_OK          = '\r'

    val UPPERCASE_PAGE: List<List<Char>> = listOf(
        listOf('A','B','C','D','E','F','G','H','I'),
        listOf('J','K','L','M','N','O','P','Q','R'),
        listOf('S','T','U','V','W','X','Y','Z',' '),
        listOf(KEY_PAGE_TOGGLE, '.', ',', '!', '?', '\u2642', '\u2640', KEY_DEL, KEY_OK),
    )

    val LOWERCASE_PAGE: List<List<Char>> = listOf(
        listOf('a','b','c','d','e','f','g','h','i'),
        listOf('j','k','l','m','n','o','p','q','r'),
        listOf('s','t','u','v','w','x','y','z',' '),
        listOf(KEY_PAGE_TOGGLE, '.', ',', '!', '?', '\u2642', '\u2640', KEY_DEL, KEY_OK),
    )

    // Pages in order; pressing PAGE_TOGGLE advances to the next page (wrapping)
    val PAGES: List<List<List<Char>>> = listOf(UPPERCASE_PAGE, LOWERCASE_PAGE)

    const val OK_ROW = 3
    const val OK_COL = 8
    const val PAGE_TOGGLE_ROW = 3
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
