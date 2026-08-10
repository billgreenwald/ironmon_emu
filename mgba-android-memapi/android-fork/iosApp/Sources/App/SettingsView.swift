import SwiftUI
import TrackerCore

/// Settings screen (Android SpeedSettingsDialog subset that maps to iOS). The tracker settings share
/// the exact NSUserDefaults keys IosTrackerSettings reads, so changes apply on the next poll tick.
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("rating_ruleset") private var ruleset = "STANDARD"
    @AppStorage("game_over_condition") private var gameOver = "LEAD_FAINTS"
    @AppStorage("split_fraction") private var split = 0.7
    // Emulator
    @AppStorage("default_speed") private var defaultSpeed = 1.0   // baseline (Android defaultFps)
    @AppStorage("ff_speed") private var ffSpeed = 2.0             // what the trigger switches to
    @AppStorage("speed_toggle_mode") private var speedToggle = false
    @AppStorage("l_as_speed") private var lAsSpeed = false
    // On-screen controls (read by GamepadView)
    @AppStorage("controls_alpha") private var controlsAlpha = 0.5
    @AppStorage("controls_scale") private var controlsScale = 1.0
    @AppStorage("invert_layout") private var invertLayout = false
    @AppStorage("hide_touch_when_pad") private var hideTouchWhenPad = true

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
                Section {
                    Picker("Default speed", selection: $defaultSpeed) {
                        Text("1×").tag(1.0); Text("2×").tag(2.0); Text("3×").tag(3.0); Text("4×").tag(4.0)
                    }
                    Picker("Fast-forward speed", selection: $ffSpeed) {
                        Text("1×").tag(1.0); Text("2×").tag(2.0); Text("3×").tag(3.0); Text("4×").tag(4.0)
                    }
                    Toggle("Fast-forward is a toggle (off = hold)", isOn: $speedToggle)
                    Toggle("Use on-screen L for fast-forward", isOn: $lAsSpeed)
                } header: {
                    Text("Speed")
                } footer: {
                    Text("The trigger switches between Default and Fast-forward speed — set Default 3× "
                       + "and Fast-forward 1× to make it a slow-mo button. Bind a controller button in "
                       + "Controller → Button mapping. Audio mutes above 1.5×.")
                }
                Section("Controller") {
                    NavigationLink {
                        KeybindingsView()
                    } label: {
                        Label("Button mapping", systemImage: "gamecontroller")
                    }
                    Toggle("Hide on-screen buttons when connected", isOn: $hideTouchWhenPad)
                }
                Section("On-screen controls") {
                    VStack(alignment: .leading) {
                        Text("Opacity — \(Int(controlsAlpha * 100))%")
                            .font(.footnote).foregroundColor(.secondary)
                        Slider(value: $controlsAlpha, in: 0.15...1.0)
                    }
                    VStack(alignment: .leading) {
                        Text("Size — \(Int(controlsScale * 100))%")
                            .font(.footnote).foregroundColor(.secondary)
                        Slider(value: $controlsScale, in: 0.7...1.4)
                    }
                    Toggle("Left-handed (swap D-pad / A-B)", isOn: $invertLayout)
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
