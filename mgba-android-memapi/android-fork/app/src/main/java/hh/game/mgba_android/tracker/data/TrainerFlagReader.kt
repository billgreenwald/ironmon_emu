package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge

/**
 * Reads trainer defeat flags from SaveBlock1.
 *
 * Each trainer has one flag bit in the game flags array at SaveBlock1 + gameFlagsOffset:
 *   byteOffset = (0x500 + trainerId) / 8
 *   bitIndex   = (0x500 + trainerId) % 8
 *
 * We batch-read all flag bytes in one call starting at offset 0xA0 within gameFlagsOffset
 * (corresponding to trainerId=0), which covers all trainer IDs across all Gen III games.
 *
 * Reference: Lua tracker TrainersOnRouteScreen.lua + RouteData.lua trainer flag logic.
 */
object TrainerFlagReader {

    // The flags chunk starts at gameFlagsOffset + FLAG_CHUNK_OFFSET within SaveBlock1.
    // FLAG_CHUNK_OFFSET = (0x500 + 0) / 8 = 0xA0 (byte offset for trainerId=0)
    private const val FLAG_CHUNK_OFFSET = 0xA0
    // Read 128 bytes — covers trainer IDs up to ~880 (sufficient for all Gen III games)
    private const val FLAG_CHUNK_SIZE = 128

    /**
     * Returns the defeat flag bytes from SaveBlock1, or null on read failure.
     * The returned array covers trainer IDs 0..~880.
     */
    private fun readFlagBytes(addresses: GameAddresses): ByteArray? {
        val saveBlock1Addr: Long = if (addresses.saveBlock1IsPointer) {
            val ptrBytes = MemoryBridge.readBytes(addresses.saveBlock1Ptr, 4) ?: return null
            ptrBytes.toLittleEndianLong().also { if (it == 0L) return null }
        } else {
            addresses.saveBlock1Ptr
        }
        val chunkAddr = saveBlock1Addr + addresses.gameFlagsOffset + FLAG_CHUNK_OFFSET
        return MemoryBridge.readBytes(chunkAddr, FLAG_CHUNK_SIZE)
    }

    /**
     * Returns true if the trainer with [trainerId] has been defeated.
     */
    private fun isDefeated(flagBytes: ByteArray, trainerId: Int): Boolean {
        val absIdx  = 0x500 + trainerId
        val byteIdx = absIdx / 8 - FLAG_CHUNK_OFFSET
        val bitIdx  = absIdx % 8
        if (byteIdx < 0 || byteIdx >= flagBytes.size) return false
        return (flagBytes[byteIdx].toInt() ushr bitIdx) and 1 != 0
    }

    /**
     * Reads all trainer defeat flags for the given [trainerTable] (mapId → trainerIds)
     * and returns a map of mapId → (defeated, total).
     * Returns an empty map if the flag bytes cannot be read.
     */
    fun readCounts(
        addresses: GameAddresses,
        trainerTable: Map<Int, List<Int>>,
    ): Map<Int, Pair<Int, Int>> {
        if (trainerTable.isEmpty()) return emptyMap()
        val flagBytes = readFlagBytes(addresses) ?: return emptyMap()
        return buildMap {
            for ((mapId, trainerIds) in trainerTable) {
                val total    = trainerIds.size
                val defeated = trainerIds.count { isDefeated(flagBytes, it) }
                put(mapId, defeated to total)
            }
        }
    }

    private fun ByteArray.toLittleEndianLong(): Long =
        (this[0].toLong() and 0xFF) or
        ((this[1].toLong() and 0xFF) shl 8) or
        ((this[2].toLong() and 0xFF) shl 16) or
        ((this[3].toLong() and 0xFF) shl 24)
}
