import Foundation
import TrackerCore

// Bridging helpers between Swift `Data`/`[UInt8]` and Kotlin's `KotlinByteArray`.
// Tracker memory reads are small (typically ≤ 100 bytes), so per-byte copying is fine.

extension Data {
    /// Build a `KotlinByteArray` from this `Data` (bytes are signed in Kotlin; reinterpret is safe).
    func toKotlinByteArray() -> KotlinByteArray {
        let arr = KotlinByteArray(size: Int32(count))
        for (i, byte) in enumerated() {
            arr.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return arr
    }
}

extension KotlinByteArray {
    /// Copy a `KotlinByteArray` into Swift `Data`.
    func toData() -> Data {
        var bytes = [UInt8]()
        bytes.reserveCapacity(Int(size))
        for i in 0..<size {
            bytes.append(UInt8(bitPattern: get(index: i)))
        }
        return Data(bytes)
    }
}
