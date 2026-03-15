# MemoryBridge

**File:** `tracker/MemoryBridge.kt`

## Purpose
Singleton bridge between GameActivity (JNI owner) and the rest of the tracker. All memory reads from the emulator core flow through here.

## Lifecycle
- **Set:** `GameActivity.onCreate()` — `MemoryBridge.reader = { addr, len -> getMemoryRange(addr, len) }`
- **Cleared:** `GameActivity.onDestroy()` — `MemoryBridge.reader = null`
- Returns `null` safely while ROM is loading (core not ready)

## Key API
```kotlin
object MemoryBridge {
    var reader: ((address: Int, length: Int) -> ByteArray?)?
    fun readBytes(address: Long, length: Int): ByteArray?
    fun readU8(address: Long): Int?      // 0–255
    fun readU16(address: Long): Int?     // little-endian, 0–65535
    fun readU32(address: Long): Long?    // little-endian, unsigned
}
```

## Notes
- All GBA addresses ≤ 0x0FFFFFFF — `Long.toInt()` cast inside is lossless
- Has no other tracker dependencies — purely a thin JNI wrapper
- JNI function declared in `GameActivity`: `private external fun getMemoryRange(address: Int, length: Int): ByteArray?`
- Implemented in C++: `runGame.cpp` (reads from mGBA memory bus)

## Troubleshooting
- **All reads return null:** Reader not set (GameActivity not yet past onCreate) or ROM not loaded
- **Reads return stale data:** Poll loop is fine; mGBA memory bus timing is acceptable for cosmetic reads
