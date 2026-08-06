import SwiftUI
import TrackerCore

/// Settings screen (Android SpeedSettingsDialog subset that maps to iOS). The tracker settings share
/// the exact NSUserDefaults keys IosTrackerSettings reads, so changes apply on the next poll tick.
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("rating_ruleset") private var ruleset = "STANDARD"
    @AppStorage("game_over_condition") private var gameOver = "LEAD_FAINTS"
    @AppStorage("split_fraction") private var split = 0.7

    var body: some View {
        NavigationView {
            Form {
                Section("Tracker") {
                    Picker("Rating ruleset", selection: $ruleset) {
                        ForEach(SettingsSwift.shared.rulesetOptions(), id: \.self) { name in
                            Text(SettingsSwift.shared.rulesetLabel(name: name)).tag(name)
                        }
                    }
                    Picker("Game over when", selection: $gameOver) {
                        ForEach(SettingsSwift.shared.gameOverOptions(), id: \.self) { name in
                            Text(SettingsSwift.shared.gameOverLabel(name: name)).tag(name)
                        }
                    }
                }
                Section("Layout") {
                    VStack(alignment: .leading) {
                        Text("Game / Tracker split — \(Int(split * 100)) / \(Int((1 - split) * 100))")
                            .font(.footnote).foregroundColor(.secondary)
                        Slider(value: $split, in: 0.5...0.85)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
        .navigationViewStyle(.stack)
    }
}
