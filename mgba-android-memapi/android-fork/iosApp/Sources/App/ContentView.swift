import SwiftUI
import UniformTypeIdentifiers
import TrackerCore

/// Landscape split: game + touch controls on the left (~70%), the tracker panel on the right
/// (~30%) — mirroring the Android layout. The tracker side is a thin renderer of the shared
/// `TrackerState`; all logic lives in :tracker-core.
struct EmulatorView: View {
    @ObservedObject var controller: EmulatorController
    let onExit: () -> Void
    @State private var showImporter = false
    @State private var showLogImporter = false
    @State private var logData: LogData?
    @State private var showLogViewer = false
    @State private var showNaming = false

    private var activeState: ActiveState? {
        guard let s = controller.trackerState else { return nil }
        return TrackerStateSwift.shared.active(state: s)
    }

    private func importLog(_ url: URL) {
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        guard let content = try? String(contentsOf: url, encoding: .utf8) else {
            controller.errorMessage = "Couldn't read the log file."; return
        }
        guard let active = activeState else {
            controller.errorMessage = "Load a ROM first so the log can be matched to the game."; return
        }
        let lines = content.components(separatedBy: .newlines)
        if let parsed = LogDataSwift.shared.parse(active: active, lines: lines) {
            logData = parsed; showLogViewer = true
        } else {
            controller.errorMessage = "Couldn't parse that randomizer log."
        }
    }

    var body: some View {
        GeometryReader { geo in
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
            .frame(width: geo.size.width * 0.7)

            Divider()

            TrackerPanel(state: controller.trackerState)
                .frame(width: geo.size.width * 0.3)
        }
        .background(Color(.systemBackground))
        .overlay(alignment: .topTrailing) {
            if controller.romLoaded {
                HStack(spacing: 2) {
                    Button { onExit() } label: { Image(systemName: "chevron.left.circle").padding(8) }
                    Button { showNaming = true } label: { Image(systemName: "keyboard").padding(8) }
                    Button { showLogImporter = true } label: { Image(systemName: "book").padding(8) }
                    Button { showImporter = true } label: { Image(systemName: "arrow.triangle.2.circlepath").padding(8) }
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
        .fileImporter(isPresented: $showLogImporter,
                      allowedContentTypes: [.data],
                      allowsMultipleSelection: false) { result in
            if case .success(let urls) = result, let url = urls.first {
                importLog(url)
            }
        }
        .fullScreenCover(isPresented: $showLogViewer) {
            if let d = logData, let active = activeState {
                LogViewerOverlay(data: d, active: active)
            }
        }
        .sheet(isPresented: $showNaming) { NamingOverlay(controller: controller) }
        .alert("Error", isPresented: Binding(
            get: { controller.errorMessage != nil },
            set: { if !$0 { controller.errorMessage = nil } })) {
            Button("OK", role: .cancel) { controller.errorMessage = nil }
        } message: {
            Text(controller.errorMessage ?? "")
        }
        }
    }
}
