import SwiftUI
import TrackerCore

/// Minimal MVP renderer of `TrackerState`. This is deliberately thin — it only reflects shared
/// state. Grow this into the SwiftUI equivalent of Android's `TrackerPanel` (party carousel,
/// battle panel, detail sheets) as the port matures.
struct ContentView: View {
    @StateObject private var viewModel: TrackerViewModel

    init(provider: MemoryProvider = MockMemoryProvider()) {
        _viewModel = StateObject(wrappedValue: TrackerViewModel(provider: provider))
    }

    var body: some View {
        NavigationView {
            content
                .navigationTitle("Ironmon Tracker")
        }
        .onAppear { viewModel.start() }
        .onDisappear { viewModel.stop() }
    }

    @ViewBuilder
    private var content: some View {
        if let active = viewModel.state as? TrackerStateActive {
            activeView(active)
        } else if viewModel.state is TrackerStateNoGameLoaded {
            statusView("No game loaded", systemImage: "gamecontroller")
        } else {
            statusView("Disconnected", systemImage: "bolt.horizontal.circle")
        }
    }

    private func activeView(_ active: TrackerStateActive) -> some View {
        List {
            Section("Run") {
                labeled("Game", "\(active.game.name)")
                labeled("Attempt", "\(active.runAttempts)")
                if active.isGameOver { labeled("Status", "GAME OVER") }
            }
            Section("Party (\(active.party.count))") {
                ForEach(Array(active.party.enumerated()), id: \.offset) { _, mon in
                    HStack {
                        Text(mon.nickname.isEmpty ? mon.speciesName : mon.nickname)
                        Spacer()
                        Text("Lv \(mon.level)").foregroundColor(.secondary)
                    }
                }
            }
            if active.battle.isActive, let enemy = active.battle.enemy {
                Section("Battle") {
                    labeled("Enemy", enemy.name)
                    labeled("Level", "\(enemy.level)")
                }
            }
        }
    }

    private func statusView(_ text: String, systemImage: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: systemImage).font(.largeTitle).foregroundColor(.secondary)
            Text(text).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func labeled(_ label: String, _ value: String) -> some View {
        HStack { Text(label); Spacer(); Text(value).foregroundColor(.secondary) }
    }
}
