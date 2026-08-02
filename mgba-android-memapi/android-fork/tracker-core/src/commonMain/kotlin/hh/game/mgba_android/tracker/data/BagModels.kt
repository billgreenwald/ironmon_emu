package hh.game.mgba_android.tracker.data

/**
 * Pure data holders for decoded bag contents. Extracted from BagReader (which stays Android-side
 * for now due to logging) so shared code can reference them from commonMain.
 */
data class BagItemEntry(val name: String, val quantity: Int)

data class BagDetailInfo(
    val hpHealPercent: Float,    // cumulative heal % of lead's max HP (capped at 9999)
    val hpHealCount: Int,        // total number of HP healing items (capped at 99)
    val hpItems: List<BagItemEntry>,
    val ppItems: List<BagItemEntry>,
    val statusItems: List<BagItemEntry>,
    val battleItems: List<BagItemEntry>,
) {
    companion object {
        val EMPTY = BagDetailInfo(0f, 0, emptyList(), emptyList(), emptyList(), emptyList())
    }
}
