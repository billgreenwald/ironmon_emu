package me.magnum.melonds

object NativeMemBridge {
    external fun getMemoryRange(address: Int, length: Int): ByteArray?
}
