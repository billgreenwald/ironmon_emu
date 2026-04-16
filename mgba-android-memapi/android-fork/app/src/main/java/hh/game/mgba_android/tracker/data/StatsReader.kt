package hh.game.mgba_android.tracker.data

import android.util.Log
import hh.game.mgba_android.tracker.MemoryBridge

data class GameStats(
    val steps: Long,
    val totalBattles: Long,
    val pokemonCenterVisits: Long,
)

object StatsReader {

    private const val TAG = "StatsReader"

    // Stat indices per Lua tracker Constants.GAME_STATS (game_stat.h)
    private const val IDX_STEPS = 5
    private const val IDX_TOTAL_BATTLES = 7
    private const val IDX_USED_POKECENTER = 15
    private const val SIZEOF_GAME_STAT = 4 // each stat is a 32-bit dword

    private var iwramScanned = false

    // Scan IWRAM 0x03003000–0x03005FFF for any dword that looks like a valid EWRAM pointer.
    // Fires once per poller session to help locate gSaveBlock1ptr on unknown ROM variants.
    fun scanIwramForEwramPointers() {
        if (iwramScanned) return
        iwramScanned = true
        val results = StringBuilder()
        var addr = 0x03003000L
        while (addr <= 0x03005FFCL) {
            val b = MemoryBridge.readBytes(addr, 4)
            if (b != null) {
                val v = (b[0].toLong() and 0xFF) or ((b[1].toLong() and 0xFF) shl 8) or
                        ((b[2].toLong() and 0xFF) shl 16) or ((b[3].toLong() and 0xFF) shl 24)
                if (v in 0x02000000L..0x0203FFFFL) {
                    results.append("0x${addr.toString(16)}→0x${v.toString(16)} ")
                }
            }
            addr += 4
        }
        Log.i(TAG, "IWRAM EWRAM-ptr scan: ${if (results.isEmpty()) "none found" else results.trim()}")
    }

    fun read(addresses: GameAddresses): GameStats? {
        // Resolve SaveBlock1 address (pointer for FR/LG/Emerald; direct address for Ruby/Sapphire)
        val saveBlock1Addr: Long = if (addresses.saveBlock1IsPointer) {
            val ptrBytes = MemoryBridge.readBytes(addresses.saveBlock1Ptr, 4)
            if (ptrBytes == null) { Log.w(TAG, "sb1 ptr read null @ 0x${addresses.saveBlock1Ptr.toString(16)}"); return null }
            val addr = ptrBytes.toLittleEndianLong()
            if (addr == 0L) {
                val hex = ptrBytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                Log.w(TAG, "sb1 ptr is 0 @ 0x${addresses.saveBlock1Ptr.toString(16)} bytes=$hex")
                scanIwramForEwramPointers()
                return null
            }
            addr
        } else {
            addresses.saveBlock1Ptr
        }

        // Get 32-bit XOR key used to encrypt game stats.
        // Ruby/Sapphire (saveBlock2Ptr == 0L) have no encryption per Lua tracker (game == 1 → return nil).
        val xorKey: Long = if (addresses.saveBlock2Ptr == 0L) {
            0L
        } else {
            val sb2PtrBytes = MemoryBridge.readBytes(addresses.saveBlock2Ptr, 4)
            if (sb2PtrBytes == null) { Log.w(TAG, "sb2 ptr read null @ 0x${addresses.saveBlock2Ptr.toString(16)}"); return null }
            val sb2Addr = sb2PtrBytes.toLittleEndianLong()
            if (sb2Addr == 0L) { Log.w(TAG, "sb2 ptr is 0 (save not initialized?)"); return null }
            val keyBytes = MemoryBridge.readBytes(sb2Addr + addresses.encryptionKeyOffset, 4)
            if (keyBytes == null) { Log.w(TAG, "xor key read null @ 0x${(sb2Addr + addresses.encryptionKeyOffset).toString(16)}"); return null }
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
