package com.ironmon.tracker.memory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TCP client for the mGBA memory read API (localhost:7777).
 *
 * Protocol (all little-endian):
 *   Request  6 bytes: [uint32 address][uint8 length][uint8 reserved=0]
 *   Response N bytes: N raw bytes (N = requested length, 1/2/4 only)
 *
 * Maintains a persistent connection and reconnects on any IOException.
 * All public methods return null on error (emulator not running, etc.).
 */
class MemoryClient(
    private val host: String = "127.0.0.1",
    private val port: Int    = 7777,
    private val connectTimeoutMs: Int = 1000,
) {
    private var socket: Socket?       = null
    private var output: OutputStream? = null
    private var input:  InputStream?  = null

    // ── Public read methods ──────────────────────────────────────────────────

    suspend fun readU8(address: Long): Int? =
        readBytes(address, 1)?.let { it[0].toInt() and 0xFF }

    suspend fun readU16(address: Long): Int? =
        readBytes(address, 2)?.let {
            (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8)
        }

    suspend fun readU32(address: Long): Long? =
        readBytes(address, 4)?.let {
            (it[0].toLong() and 0xFF) or
            ((it[1].toLong() and 0xFF) shl 8) or
            ((it[2].toLong() and 0xFF) shl 16) or
            ((it[3].toLong() and 0xFF) shl 24)
        }

    /**
     * Read [length] bytes (must be 1, 2, or 4) from the given GBA address.
     * Returns null on any error (caller interprets as disconnected).
     */
    suspend fun readBytes(address: Long, length: Int): ByteArray? = withContext(Dispatchers.IO) {
        require(length == 1 || length == 2 || length == 4) {
            "length must be 1, 2, or 4 — got $length"
        }
        try {
            ensureConnected()
            sendRequest(address, length)
            receiveResponse(length)
        } catch (e: Exception) {
            Log.e("MemoryClient", "readBytes(0x${address.toString(16)}, $length) failed: ${e.javaClass.simpleName}: ${e.message}")
            closeQuietly()
            null
        }
    }

    fun close() {
        closeQuietly()
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun ensureConnected() {
        if (socket?.isConnected == true && socket?.isClosed == false) return
        val s = Socket()
        s.connect(InetSocketAddress(host, port), connectTimeoutMs)
        s.soTimeout = 2000
        socket = s
        output = s.getOutputStream()
        input  = s.getInputStream()
    }

    private fun sendRequest(address: Long, length: Int) {
        val buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(address.toInt())  // uint32 — safe because GBA addresses fit in 32 bits
        buf.put(length.toByte())
        buf.put(0x00.toByte())       // reserved
        output!!.write(buf.array())
        output!!.flush()
    }

    private fun receiveResponse(length: Int): ByteArray {
        val result = ByteArray(length)
        var received = 0
        while (received < length) {
            val n = input!!.read(result, received, length - received)
            if (n < 0) throw IOException("Server closed connection during response")
            received += n
        }
        return result
    }

    private fun closeQuietly() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        output = null
        input  = null
    }
}
