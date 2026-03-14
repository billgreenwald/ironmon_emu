package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge

data class GameStats(
    val steps: Long,
    val totalBattles: Long,
    val pokemonCenterVisits: Long,
)

object StatsReader {

    // Stat indices per Lua tracker Constants.GAME_STATS (game_stat.h)
    private const val IDX_STEPS = 5
    private const val IDX_TOTAL_BATTLES = 7
    private const val IDX_USED_POKECENTER = 15
    private const val SIZEOF_GAME_STAT = 4 // each stat is a 32-bit dword

    fun read(addresses: GameAddresses): GameStats? {
        // Resolve SaveBlock1 address (pointer for FR/LG/Emerald; direct address for Ruby/Sapphire)
        val saveBlock1Addr: Long = if (addresses.saveBlock1IsPointer) {
            val ptrBytes = MemoryBridge.readBytes(addresses.saveBlock1Ptr, 4) ?: return null
            val addr = ptrBytes.toLittleEndianLong()
            if (addr == 0L) return null
            addr
        } else {
            addresses.saveBlock1Ptr
        }

        // Get 32-bit XOR key used to encrypt game stats.
        // Ruby/Sapphire (saveBlock2Ptr == 0L) have no encryption per Lua tracker (game == 1 → return nil).
        val xorKey: Long = if (addresses.saveBlock2Ptr == 0L) {
            0L
        } else {
            val sb2PtrBytes = MemoryBridge.readBytes(addresses.saveBlock2Ptr, 4) ?: return null
            val sb2Addr = sb2PtrBytes.toLittleEndianLong()
            if (sb2Addr == 0L) return null
            val keyBytes = MemoryBridge.readBytes(sb2Addr + addresses.encryptionKeyOffset, 4) ?: return null
            keyBytes.toLittleEndianLong()
        }

        val statsBase = saveBlock1Addr + addresses.gameStatsOffset

        fun readStat(idx: Int): Long {
            val bytes = MemoryBridge.readBytes(statsBase + idx * SIZEOF_GAME_STAT, 4) ?: return 0L
            val raw = bytes.toLittleEndianLong()
            return if (xorKey == 0L) raw else (raw xor xorKey) and 0xFFFFFFFFL
        }

        return GameStats(
            steps               = readStat(IDX_STEPS),
            totalBattles        = readStat(IDX_TOTAL_BATTLES),
            pokemonCenterVisits = readStat(IDX_USED_POKECENTER),
        )
    }

    private fun ByteArray.toLittleEndianLong(): Long =
        (this[0].toLong() and 0xFF) or
        ((this[1].toLong() and 0xFF) shl 8) or
        ((this[2].toLong() and 0xFF) shl 16) or
        ((this[3].toLong() and 0xFF) shl 24)
}
