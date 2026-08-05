import SwiftUI
import TrackerCore

/// Auto-name overlay (Android NamingOverlay): type a name, replay it onto the GBA naming screen.
struct NamingOverlay: View {
    @ObservedObject var controller: EmulatorController
    @State private var name = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("Open the in-game naming screen first (cursor on 'A'), then type a name and tap Type.")
                    .font(.footnote).foregroundColor(.secondary).multilineTextAlignment(.center)
                TextField("Name (≤ 10 chars)", text: $name)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .onChange(of: name) { newValue in
                        if newValue.count > 10 { name = String(newValue.prefix(10)) }
                    }
                Button("Type on GBA") {
                    controller.typeName(name)
                    dismiss()
                }
                .disabled(name.isEmpty)
                .padding(.horizontal, 20).padding(.vertical, 10)
                .background(name.isEmpty ? Color.gray : TrackerTheme.accentBlue, in: Capsule())
                .foregroundColor(.white)
                Spacer()
            }
            .padding()
            .navigationTitle("Auto-Name")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
        }
        .navigationViewStyle(.stack)
    }
}
