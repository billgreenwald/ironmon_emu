import Foundation
import TrackerCore

/// Supplies raw guest-memory bytes to the shared tracker — the iOS analogue of Android's JNI
/// `getMemoryRange`. Backed by the live mGBA core.
protocol MemoryProvider: AnyObject {
    /// Return `length` bytes at guest `address`, or nil if the core isn't live / the read fails.
    func read(address: Int32, length: Int32) -> Data?
}

/// Installs a provider as the shared `MemoryBridge` reader. The reader returns a `KotlinByteArray?`
/// (the type `MemoryBridge.reader` expects), built from the provider's `Data`.
enum MemoryBridgeInstaller {
    static func install(_ provider: MemoryProvider) {
        // address/length arrive boxed as KotlinInt (function-type params box primitives).
        IosTracker.shared.setMemoryReader { address, length in
            provider.read(address: address.int32Value, length: length.int32Value)?.toKotlinByteArray()
        }
    }
}

/// Reads guest memory from the running mGBA core (lock-free rawRead8/busRead8, tear-tolerant —
/// exactly like Android's getMemoryRange).
final class MgbaMemoryProvider: MemoryProvider {
    private let core: EmulatorCore
    init(core: EmulatorCore) { self.core = core }

    func read(address: Int32, length: Int32) -> Data? {
        core.readMemory(atAddress: UInt32(bitPattern: address), length: UInt32(length))
    }
}
