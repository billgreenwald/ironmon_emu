import SwiftUI
import TrackerCore

/// The single owner tying the emulator core, audio, and the shared tracker together, and the
/// SwiftUI source of truth. All tracker logic stays in :tracker-core; this only wires + forwards.
@MainActor
final class EmulatorController: ObservableObject {
    @Published private(set) var trackerState: TrackerState?   // nil until the first poll emits
    @Published private(set) var romLoaded = false
    @Published var errorMessage: String?
    @Published private(set) var fastForward = false

    let core = EmulatorCore()
    private lazy var audio = EmulatorAudio(core: core)
    private var observer: Kotlinx_coroutines_coreJob?
    // Touch controls and a hardware gamepad each own a mask; the core sees their union, so a
    // physical button and an on-screen button never clobber each other's held state.
    private var touchMask: UInt32 = 0
    private var padMask: UInt32 = 0
    private func applyKeys() { core.setKeys(touchMask | padMask) }

    /// Import a picked ROM from outside the sandbox, then open it.
    func loadROM(url: URL) {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }

        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dest = docs.appendingPathComponent(url.lastPathComponent)
        if FileManager.default.fileExists(atPath: dest.path) {
            try? FileManager.default.removeItem(at: dest)
        }
        do {
            try FileManager.default.copyItem(at: url, to: dest)
        } catch {
            errorMessage = "Couldn't import ROM: \(error.localizedDescription)"
            return
        }
        openROM(path: dest.path)
    }

    /// Open a ROM already at an accessible local path (e.g. copied into the sandbox by the library).
    /// Safe to call when another ROM is loaded — switches ROMs in place.
    func openROM(path: String) {
        if romLoaded { teardown() }
        guard core.loadROM(atPath: path) else {
            errorMessage = "mGBA couldn't load this ROM."
            return
        }
        let env = IosTracker.shared.install()
        MemoryBridgeInstaller.install(MgbaMemoryProvider(core: core))
        IosTracker.shared.start(environment: env, romPath: path)
        observer = IosTracker.shared.observeState { [weak self] state in
            Task { @MainActor in self?.trackerState = state }
        }
        audio.start()
        core.start()
        romLoaded = true
        errorMessage = nil
    }

    /// Stop the current game and return to the library.
    func closeROM() {
        teardown()
        romLoaded = false
        trackerState = nil
    }

    private func teardown() {
        core.stop()
        audio.stop()
        observer?.cancel(cause: nil)
        observer = nil
        IosTracker.shared.stop()
    }

    func press(_ b: GBAButton) { touchMask |= b.rawValue; applyKeys() }
    func release(_ b: GBAButton) { touchMask &= ~b.rawValue; applyKeys() }

    /// Full GBA key bitmask from a hardware gamepad (rebuilt each input event by the controller layer).
    func setPadKeys(_ mask: UInt32) { padMask = mask; applyKeys() }

    /// Fast-forward toggle. The multiplier comes from Settings ("ff_speed", default 2×).
    func toggleFastForward() {
        fastForward.toggle()
        let stored = UserDefaults.standard.double(forKey: "ff_speed")
        let mult = Float(stored >= 1 ? stored : 2)   // 0 = key never set → default 2×
        core.setSpeedMultiplier(fastForward ? mult : 1)
    }

    /// Auto-type [name] on the Gen III naming screen by replaying the shared key sequence.
    /// Assumes the naming screen is open with the cursor at page 0 / row 0 / col 0.
    /// NOTE: press/settle timings are best-guesses — may need on-device tuning.
    func typeName(_ name: String) {
        let keys = NamingSequence.shared.keysFor(name: name)
        Task { @MainActor in
            for k in keys {
                core.setKeys(UInt32(1) << UInt32(k.code))
                try? await Task.sleep(nanoseconds: 90_000_000)                       // hold ~90ms
                core.setKeys(0)
                try? await Task.sleep(nanoseconds: k.longDelay ? 550_000_000 : 60_000_000)
            }
            applyKeys()   // restore any held touch / gamepad input
        }
    }

    func shutdown() { teardown() }
}
