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
