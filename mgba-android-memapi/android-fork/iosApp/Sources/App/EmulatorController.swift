import SwiftUI
import TrackerCore

/// The single owner tying the emulator core, audio, and the shared tracker together, and the
/// SwiftUI source of truth. All tracker logic stays in :tracker-core; this only wires + forwards.
@MainActor
final class EmulatorController: ObservableObject {
    @Published private(set) var trackerState: TrackerState?   // nil until the first poll emits
    @Published private(set) var romLoaded = false
    @Published var errorMessage: String?

    let core = EmulatorCore()
    private lazy var audio = EmulatorAudio(core: core)
    private var observer: Kotlinx_coroutines_coreJob?
    private var keyMask: UInt32 = 0

    /// Import a picked ROM into the sandbox, boot the core, wire the tracker, and start audio.
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

        guard core.loadROM(atPath: dest.path) else {
            errorMessage = "mGBA couldn't load this ROM."
            return
        }

        // Wire the shared tracker to read from the live core.
        let env = IosTracker.shared.install()
        MemoryBridgeInstaller.install(MgbaMemoryProvider(core: core))
        IosTracker.shared.start(environment: env, romPath: dest.path)
        observer = IosTracker.shared.observeState { [weak self] state in
            Task { @MainActor in self?.trackerState = state }
        }

        audio.start()
        core.start()
        romLoaded = true
        errorMessage = nil
    }

    func press(_ b: GBAButton) { keyMask |= b.rawValue; core.setKeys(keyMask) }
    func release(_ b: GBAButton) { keyMask &= ~b.rawValue; core.setKeys(keyMask) }

    func shutdown() {
        core.stop()
        audio.stop()
        observer?.cancel(cause: nil)
        observer = nil
        IosTracker.shared.stop()
    }
}
