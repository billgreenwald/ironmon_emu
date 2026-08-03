import Foundation
import TrackerCore

/// Supplies raw guest-memory bytes to the shared tracker. This is the iOS analogue of the Android
/// JNI `getMemoryRange` path — it must call the live mGBA core's `rawRead8` (ROM region) /
/// `busRead8` (RAM region), exactly like `runGame.cpp` does on Android.
protocol MemoryProvider: AnyObject {
    /// Return `length` bytes starting at guest `address`, or nil if the core isn't live / read fails.
    func read(address: Int32, length: Int32) -> Data?
}

/// Installs a provider as the shared `MemoryBridge` reader. The reader closure returns a
/// `KotlinByteArray?` — the exact type `MemoryBridge.reader` expects — built from the provider's Data.
enum MemoryBridgeInstaller {
    static func install(_ provider: MemoryProvider) {
        IosTracker.shared.setMemoryReader { address, length in
            provider.read(address: address, length: length)?.toKotlinByteArray()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Placeholder provider so the app builds and runs *before* the mGBA core is wired in. It always
// returns nil, so the tracker shows "Disconnected". Swap this for `MgbaMemoryProvider` once the
// emulator base is integrated (see iosApp/README.md, "Wiring the emulator").
// ─────────────────────────────────────────────────────────────────────────────────────────────
final class MockMemoryProvider: MemoryProvider {
    func read(address: Int32, length: Int32) -> Data? { nil }
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Real provider skeleton. Fill in `corePointer` with the running `mCore *` from the chosen iOS
// mGBA base, then implement the byte loop mirroring runGame.cpp:762-772.
//
//   ROM region 0x08000000..<0x0E000000  -> core.pointee.rawRead8(core, addr, -1)
//   everything else (RAM/IO)            -> core.pointee.busRead8(core, addr)
//
// Guard on the core being live (equivalent of Android's mCoreThreadIsActive).
// ─────────────────────────────────────────────────────────────────────────────────────────────
/*
import mGBA   // whatever module exposes `mCore` from the forked base

final class MgbaMemoryProvider: MemoryProvider {
    /// The live core pointer. Set when a ROM is loaded/running; cleared on unload.
    weak var emulator: EmulatorHost?   // your wrapper that owns the `mCore *` + "is running" flag

    func read(address: Int32, length: Int32) -> Data? {
        guard let core = emulator?.corePointer, emulator?.isRunning == true else { return nil }
        var out = Data(count: Int(length))
        let romStart: UInt32 = 0x0800_0000
        let romEnd:   UInt32 = 0x0E00_0000
        out.withUnsafeMutableBytes { raw in
            let dst = raw.bindMemory(to: UInt8.self)
            for i in 0..<Int(length) {
                let addr = UInt32(bitPattern: address) &+ UInt32(i)
                let byte: UInt32 = (addr >= romStart && addr < romEnd)
                    ? core.pointee.rawRead8(core, addr, -1)
                    : core.pointee.busRead8(core, addr)
                dst[i] = UInt8(truncatingIfNeeded: byte)
            }
        }
        return out
    }
}
*/
