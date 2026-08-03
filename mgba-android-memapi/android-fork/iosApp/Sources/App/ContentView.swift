import SwiftUI
import UniformTypeIdentifiers
import TrackerCore

/// Landscape split: game + touch controls on the left (~70%), the tracker panel on the right
/// (~30%) — mirroring the Android layout. The tracker side is a thin renderer of the shared
/// `TrackerState`; all logic lives in :tracker-core.
struct ContentView: View {
    @StateObject private var controller = EmulatorController()
    @State private var showImporter = false

    var body: some View {
        HStack(spacing: 0) {
            // Left — game + controls
            VStack(spacing: 0) {
                ZStack {
                    Color.black
                    if controller.romLoaded {
                        GameMetalView(core: controller.core)
                            .aspectRatio(240.0 / 160.0, contentMode: .fit)
                    } else {
                        Button { showImporter = true } label: {
                            Label("Import GBA ROM", systemImage: "square.and.arrow.down")
                                .padding(.horizontal, 16).padding(.vertical, 10)
                                .background(.ultraThinMaterial, in: Capsule())
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                GamepadView(controller: controller)
                    .frame(height: 190)
            }
            .frame(maxWidth: .infinity)

            Divider()

            TrackerPanel(state: controller.trackerState)
                .frame(width: 280)
        }
        .background(Color(.systemBackground))
        .overlay(alignment: .topTrailing) {
            if controller.romLoaded {
                Button { showImporter = true } label: {
                    Image(systemName: "arrow.triangle.2.circlepath").padding(10)
                }
            }
        }
        .fileImporter(isPresented: $showImporter,
                      allowedContentTypes: [.data],
                      allowsMultipleSelection: false) { result in
            if case .success(let urls) = result, let url = urls.first {
                controller.loadROM(url: url)
            }
        }
        .alert("Error", isPresented: Binding(
            get: { controller.errorMessage != nil },
            set: { if !$0 { controller.errorMessage = nil } })) {
            Button("OK", role: .cancel) { controller.errorMessage = nil }
        } message: {
            Text(controller.errorMessage ?? "")
        }
    }
}

/// Thin renderer of the shared `TrackerState`. MVP: run info, party, and active-battle enemy.
struct TrackerPanel: View {
    let state: TrackerState

    var body: some View {
        if let active = state as? TrackerStateActive {
            List {
                Section("Run") {
                    row("Game", "\(active.game.name)")
                    row("Attempt", "\(active.runAttempts)")
                    if active.isGameOver { row("Status", "GAME OVER") }
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
                        row("Enemy", enemy.name)
                        row("Level", "\(enemy.level)")
                    }
                }
            }
            .listStyle(.plain)
        } else if state is TrackerStateNoGameLoaded {
            status("No game loaded", "gamecontroller")
        } else {
            status("Waiting for game…", "bolt.horizontal.circle")
        }
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack { Text(label); Spacer(); Text(value).foregroundColor(.secondary) }
    }

    private func status(_ text: String, _ symbol: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: symbol).font(.largeTitle).foregroundColor(.secondary)
            Text(text).foregroundColor(.secondary).font(.footnote)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
