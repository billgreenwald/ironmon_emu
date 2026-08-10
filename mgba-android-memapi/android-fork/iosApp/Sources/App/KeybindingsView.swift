import SwiftUI
import GameController

/// Remap each physical controller button to a GBA button. Pure UserDefaults editing (via
/// `ControllerBindings`); the live `GameControllerManager` re-reads on `UserDefaults.didChange`,
/// so edits take effect immediately without holding a reference to it here.
struct KeybindingsView: View {
    @State private var connectedName: String? = GCController.controllers().first?.vendorName
    // Bump to force the Pickers to re-read after edits / reset.
    @State private var revision = 0

    private let bindings = ControllerBindings()

    var body: some View {
        Form {
            Section {
                HStack {
                    Image(systemName: "gamecontroller")
                    Text(connectedName ?? "No controller connected")
                        .foregroundColor(connectedName == nil ? .secondary : .primary)
                }
            } footer: {
                Text("Pair an Xbox, PlayStation, or MFi controller in iOS Settings → Bluetooth. "
                   + "The D-pad and left stick always control movement.")
            }

            Section("Button mapping") {
                ForEach(PadInput.allCases) { input in
                    Picker(input.displayName, selection: binding(for: input)) {
                        ForEach(GBATarget.allCases) { t in Text(t.label).tag(t) }
                    }
                }
            }
            .id(revision)

            Section {
                Button("Reset to defaults", role: .destructive) {
                    ControllerBindings.resetToDefaults()
                    revision += 1
                }
            }
        }
        .navigationTitle("Controller Buttons")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(NotificationCenter.default.publisher(for: .GCControllerDidConnect)) { _ in
            connectedName = GCController.controllers().first?.vendorName
        }
        .onReceive(NotificationCenter.default.publisher(for: .GCControllerDidDisconnect)) { _ in
            connectedName = GCController.controllers().first?.vendorName
        }
    }

    private func binding(for input: PadInput) -> Binding<GBATarget> {
        Binding(
            get: { bindings.target(input) },
            set: { bindings.setTarget($0, for: input) }
        )
    }
}
