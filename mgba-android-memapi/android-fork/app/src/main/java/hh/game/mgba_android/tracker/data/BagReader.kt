package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge

data class HealInfo(
    val healPercent: Float,  // cumulative heal % of lead's max HP (capped at 9999, matching Lua)
    val healCount: Int,      // total number of HP healing items (capped at 99, matching Lua)
)

/**
 * Reads the player's bag from SaveBlock1 and calculates total healing percentage.
 *
 * Matches Lua tracker Program.updateBagItems() + Program.recalcLeadPokemonHealingInfo().
 * Item list mirrors MiscData.HealingItems in MiscData.lua.
 */
object BagReader {

    // Healing item types (matches Lua MiscData.HealingType)
    private const val CONSTANT   = true   // heals a fixed HP amount
    private const val PERCENTAGE = false  // heals a % of max HP

    // Healing items from Lua tracker MiscData.lua MiscData.HealingItems (lines 150–311)
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

    /**
     * @param addresses  Per-game address table
     * @param maxHp      Lead Pokemon's max HP (needed to convert constant heals to %)
     */
    fun read(addresses: GameAddresses, maxHp: Int): HealInfo {
        if (maxHp <= 0) return HealInfo(0f, 0)

        // Resolve SaveBlock1 pointer (same pattern as StatsReader)
        val sb1Bytes = MemoryBridge.readBytes(addresses.saveBlock1Ptr, 4) ?: return HealInfo(0f, 0)
        val saveBlock1Addr = sb1Bytes.toLittleEndianLong()
        if (saveBlock1Addr == 0L) return HealInfo(0f, 0)

        // 16-bit XOR encryption key for item quantities.
        // Ruby/Sapphire (saveBlock2Ptr == 0L) have no encryption — matches Lua getEncryptionKey check.
        // Lua uses getEncryptionKey(2) = 16-bit key, so we read 2 bytes at SaveBlock2+offset.
        val encKey16: Int? = if (addresses.saveBlock2Ptr != 0L) {
            val sb2Bytes = MemoryBridge.readBytes(addresses.saveBlock2Ptr, 4) ?: return HealInfo(0f, 0)
            val sb2Addr = sb2Bytes.toLittleEndianLong()
            if (sb2Addr == 0L) return HealInfo(0f, 0)
            MemoryBridge.readU16(sb2Addr + addresses.encryptionKeyOffset)
        } else null

        var healingTotal = 0
        var healingPercentage = 0f

        // Scan Items pocket and Berries pocket — matches Lua addressesToScan
        val pockets = listOf(
            saveBlock1Addr + addresses.bagPocket_Items_offset   to addresses.bagPocket_Items_size,
            saveBlock1Addr + addresses.bagPocket_Berries_offset to addresses.bagPocket_Berries_size,
        )

        for ((pocketAddr, pocketSize) in pockets) {
            // Each slot = 4 bytes: u16 itemId | u16 quantity
            val raw = MemoryBridge.readBytes(pocketAddr, pocketSize * 4) ?: continue
            for (i in 0 until pocketSize) {
                val base = i * 4
                val itemId = (raw[base].toInt() and 0xFF) or ((raw[base + 1].toInt() and 0xFF) shl 8)
                var quantity = (raw[base + 2].toInt() and 0xFF) or ((raw[base + 3].toInt() and 0xFF) shl 8)
                if (encKey16 != null) quantity = quantity xor encKey16
                // Sanity cap of 999 matching Lua: "if quantity <= 999 then"
                if (quantity <= 0 || quantity > 999) continue

                val healItem = HEALING_ITEMS[itemId] ?: continue

                // Lua recalcLeadPokemonHealingInfo() logic exactly:
                val percentAmt = if (healItem.isConstant) {
                    // quantity * math.min(healItemData.amount / maxHP * 100, 100)
                    quantity * minOf(healItem.amount / maxHp * 100f, 100f)
                } else {
                    // quantity * healItemData.amount
                    quantity * healItem.amount
                }
                healingTotal += quantity
                healingPercentage += percentAmt
            }
        }

        return HealInfo(
            healPercent = healingPercentage.coerceAtMost(9999f),  // Lua max of 9999
            healCount   = healingTotal.coerceAtMost(99),           // Lua max of 99
        )
    }

    private fun ByteArray.toLittleEndianLong(): Long =
        (this[0].toLong() and 0xFF) or
        ((this[1].toLong() and 0xFF) shl 8) or
        ((this[2].toLong() and 0xFF) shl 16) or
        ((this[3].toLong() and 0xFF) shl 24)
}
