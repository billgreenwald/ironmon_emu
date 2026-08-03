import Foundation
import Combine
import TrackerCore

/// Bridges the shared `TrackerPoller` state flow into an `ObservableObject` SwiftUI can render.
/// All business logic stays in `:tracker-core`; this only forwards state to the UI.
@MainActor
final class TrackerViewModel: ObservableObject {
    @Published private(set) var state: TrackerState = TrackerStateDisconnected()

    private var observerJob: Kotlinx_coroutines_coreJob?
    private let provider: MemoryProvider

    init(provider: MemoryProvider) {
        self.provider = provider
    }

    /// Install seams, wire the memory reader, start polling, and subscribe to state.
    /// - Parameter romPath: full path of the loaded ROM (for randomizer-log lookup); nil is fine.
    func start(romPath: String? = nil) {
        let env = IosTracker.shared.install()
        MemoryBridgeInstaller.install(provider)
        IosTracker.shared.start(environment: env, romPath: romPath)
        observerJob = IosTracker.shared.observeState { [weak self] newState in
            // observeState collects on Dispatchers.Main, but hop explicitly for safety.
            Task { @MainActor in self?.state = newState }
        }
    }

    func stop() {
        observerJob?.cancel(cause: nil)
        observerJob = nil
        IosTracker.shared.stop()
    }

    // UI actions delegate straight to the shared poller.
    func rerollBall() { IosTracker.shared.rerollBall() }
    func nextRun() { IosTracker.shared.manualNextRun() }
}
