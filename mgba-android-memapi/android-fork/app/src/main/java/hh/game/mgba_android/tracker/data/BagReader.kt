package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.tables.ItemTable

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

/**
 * Reads the player's bag from SaveBlock1 and calculates total healing percentage + categorized lists.
 *
 * Matches Lua tracker Program.updateBagItems() + Program.recalcLeadPokemonHealingInfo().
 * Item lists mirror MiscData.lua: HealingItems, PPItems, StatusItems, BattleItems.
 */
object BagReader {

    // Healing item types (matches Lua MiscData.HealingType)
    private const val CONSTANT   = true   // heals a fixed HP amount
    private const val PERCENTAGE = false  // heals a % of max HP

    // Healing items from Lua tracker MiscData.lua MiscData.HealingItems
    private data class HealItem(val amount: Float, val isConstant: Boolean)

    private val HEALING_ITEMS: Map<Int, HealItem> = mapOf(
        13  to HealItem(20f,    CONSTANT),    // Potion
        19  to HealItem(100f,   PERCENTAGE),  // Full Restore
        20  to HealItem(100f,   PERCENTAGE),  // Max Potion
        21  to HealItem(200f,   CONSTANT),    // Hyper Potion
        22  to HealItem(50f,    CONSTANT),    // Super Potion
        26  to HealItem(50f,    CONSTANT),    // Fresh Water
        27  to HealItem(60f,    CONSTANT),    // Soda Pop
        28  to HealItem(80f,    CONSTANT),    // Lemonade
        29  to HealItem(100f,   CONSTANT),    // Moomoo Milk
        30  to HealItem(50f,    CONSTANT),    // EnergyPowder
        31  to HealItem(200f,   CONSTANT),    // Energy Root
        44  to HealItem(20f,    CONSTANT),    // Berry Juice
        139 to HealItem(10f,    CONSTANT),    // Oran Berry
        142 to HealItem(30f,    CONSTANT),    // Sitrus Berry
        143 to HealItem(12.5f,  PERCENTAGE),  // Figy Berry
        144 to HealItem(12.5f,  PERCENTAGE),  // Wiki Berry
        145 to HealItem(12.5f,  PERCENTAGE),  // Mago Berry
        146 to HealItem(12.5f,  PERCENTAGE),  // Aguav Berry
        147 to HealItem(12.5f,  PERCENTAGE),  // Iapapa Berry
        175 to HealItem(12.5f,  PERCENTAGE),  // Enigma Berry
    )

    // PP items from Lua MiscData.PPItems
    private val PP_ITEM_IDS: Set<Int> = setOf(34, 35, 36, 37, 138)  // Ether, Max Ether, Elixir, Max Elixir, Leppa Berry

    // Status items from Lua MiscData.StatusItems
    private val STATUS_ITEM_IDS: Set<Int> = setOf(
        14, 15, 16, 17, 18, 19, 23, 32, 38,              // Antidote, Burn Heal, Ice Heal, Awakening, Parlyz Heal, Full Restore, Full Heal, Heal Powder, Lava Cookie
        133, 134, 135, 136, 137, 140, 141,               // Cheri, Chesto, Pecha, Rawst, Aspear, Persim, Lum Berry
    )

    // Battle items from Lua MiscData.BattleItems (39-41 flutes, 73-79 X items + Dire Hit)
    private val BATTLE_ITEM_IDS: Set<Int> = (39..41).toHashSet<Int>().also { it.addAll(73..79) }

    /**
     * @param addresses  Per-game address table
     * @param maxHp      Lead Pokemon's max HP (needed to convert constant heals to %)
     */
    fun read(addresses: GameAddresses, maxHp: Int): BagDetailInfo {
        if (maxHp <= 0) return BagDetailInfo.EMPTY

        // Resolve SaveBlock1 address (pointer-to-pointer for FR/LG/Emerald; direct addr for Ruby/Sapphire)
        val saveBlock1Addr: Long = if (addresses.saveBlock1IsPointer) {
            val sb1Bytes = MemoryBridge.readBytes(addresses.saveBlock1Ptr, 4)
                ?: return BagDetailInfo.EMPTY
            val addr = sb1Bytes.toLittleEndianLong()
            if (addr == 0L) return BagDetailInfo.EMPTY
            addr
        } else {
            addresses.saveBlock1Ptr
        }

        // 16-bit XOR encryption key for item quantities.
        // Ruby/Sapphire (saveBlock2Ptr == 0L) have no encryption — matches Lua getEncryptionKey check.
        val encKey16: Int? = if (addresses.saveBlock2Ptr != 0L) {
            val sb2Bytes = MemoryBridge.readBytes(addresses.saveBlock2Ptr, 4)
                ?: return BagDetailInfo.EMPTY
            val sb2Addr = sb2Bytes.toLittleEndianLong()
            if (sb2Addr == 0L) return BagDetailInfo.EMPTY
            MemoryBridge.readU16(sb2Addr + addresses.encryptionKeyOffset)
        } else null

        var healingTotal = 0
        var healingPercentage = 0f

        val hpItemList     = mutableListOf<BagItemEntry>()
        val ppItemList     = mutableListOf<BagItemEntry>()
        val statusItemList = mutableListOf<BagItemEntry>()
        val battleItemList = mutableListOf<BagItemEntry>()

        // Scan Items pocket and Berries pocket — matches Lua addressesToScan
        val pockets = listOf(
            saveBlock1Addr + addresses.bagPocket_Items_offset   to addresses.bagPocket_Items_size,
            saveBlock1Addr + addresses.bagPocket_Berries_offset to addresses.bagPocket_Berries_size,
        )

        for ((pocketAddr, pocketSize) in pockets) {
            val raw = MemoryBridge.readBytes(pocketAddr, pocketSize * 4) ?: continue
            for (i in 0 until pocketSize) {
                val base = i * 4
                val itemId = (raw[base].toInt() and 0xFF) or ((raw[base + 1].toInt() and 0xFF) shl 8)
                var quantity = (raw[base + 2].toInt() and 0xFF) or ((raw[base + 3].toInt() and 0xFF) shl 8)
                if (encKey16 != null) quantity = quantity xor encKey16
                if (quantity <= 0 || quantity > 999) continue
                if (itemId == 0) continue

                val itemName = ItemTable.get(itemId)

                // HP healing
                val healItem = HEALING_ITEMS[itemId]
                if (healItem != null) {
                    val percentAmt = if (healItem.isConstant) {
                        quantity * minOf(healItem.amount / maxHp * 100f, 100f)
                    } else {
                        quantity * healItem.amount
                    }
                    healingTotal += quantity
                    healingPercentage += percentAmt
                    hpItemList.add(BagItemEntry(itemName, quantity))
                }

                // PP items
                if (itemId in PP_ITEM_IDS) {
                    ppItemList.add(BagItemEntry(itemName, quantity))
                }

                // Status items
                if (itemId in STATUS_ITEM_IDS) {
                    statusItemList.add(BagItemEntry(itemName, quantity))
                }

                // Battle items
                if (itemId in BATTLE_ITEM_IDS) {
                    battleItemList.add(BagItemEntry(itemName, quantity))
                }
            }
        }

        return BagDetailInfo(
            hpHealPercent = healingPercentage.coerceAtMost(9999f),
            hpHealCount   = healingTotal.coerceAtMost(99),
            hpItems       = hpItemList,
            ppItems       = ppItemList,
            statusItems   = statusItemList,
            battleItems   = battleItemList,
        )
    }

    private fun ByteArray.toLittleEndianLong(): Long =
        (this[0].toLong() and 0xFF) or
        ((this[1].toLong() and 0xFF) shl 8) or
        ((this[2].toLong() and 0xFF) shl 16) or
        ((this[3].toLong() and 0xFF) shl 24)
}
