package hh.game.mgba_android.tracker

/**
 * Singleton bridge between GameActivity (JNI owner) and TrackerPoller.
 *
 * GameActivity.onCreate() sets [reader] to a lambda wrapping getMemoryRange().
 * GameActivity.onDestroy() clears [reader] to null so polling stops gracefully.
 *
 * All GBA addresses fit in a signed Int (max 0x0FFFFFFF < Int.MAX_VALUE 0x7FFFFFFF),
 * so Long→Int truncation in readBytes() is lossless.
 */
object MemoryBridge {

    @Volatile
    var reader: ((address: Int, length: Int) -> ByteArray?)? = null

    fun readBytes(address: Long, length: Int): ByteArray? =
        reader?.invoke(address.toInt(), length)

    fun readU8(address: Long): Int? =
        readBytes(address, 1)?.let { it[0].toInt() and 0xFF }

    fun readU16(address: Long): Int? =
        readBytes(address, 2)?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        }

    fun readU32(address: Long): Long? =
        readBytes(address, 4)?.let {
            (it[0].toLong() and 0xFF)         or
            ((it[1].toLong() and 0xFF) shl 8)  or
            ((it[2].toLong() and 0xFF) shl 16) or
            ((it[3].toLong() and 0xFF) shl 24)
        }
}
