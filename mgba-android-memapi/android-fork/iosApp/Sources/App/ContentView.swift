import SwiftUI
import UniformTypeIdentifiers
import TrackerCore

/// Landscape split: game + touch controls on the left (~70%), the tracker panel on the right
/// (~30%) — mirroring the Android layout. The tracker side is a thin renderer of the shared
/// `TrackerState`; all logic lives in :tracker-core.
struct EmulatorView: View {
    @ObservedObject var controller: EmulatorController
    @ObservedObject var pads: GameControllerManager
    let onExit: () -> Void
    var onQuickload: ((Bool) -> Void)? = nil
    @State private var showSettings = false
    @AppStorage("split_fraction") private var split = 0.7
    @AppStorage("hide_touch_when_pad") private var hideTouchWhenPad = true
    @AppStorage("tracker_collapsible") private var trackerCollapsible = false
    @State private var trackerCollapsed = false
    @State private var logData: LogData?
    @State private var showLogViewer = false
    @State private var showNaming = false
    @State private var importKind: ImportKind?

    private enum ImportKind { case rom, log }

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
            // When the tracker is collapsible and collapsed, the game takes the full width.
            let collapsed = trackerCollapsible && trackerCollapsed
            let gameFraction = collapsed ? 1.0 : split
            HStack(spacing: 0) {
                // Left — game fills the area, touch controls OVERLAID on top (transparent).
                ZStack {
                    Color.black
                    GameMetalView(core: controller.core)
                        .aspectRatio(240.0 / 160.0, contentMode: .fit)
                    // Hide the on-screen pad when a hardware controller is driving input (opt-out in Settings).
                    if !(hideTouchWhenPad && pads.connectedName != nil) {
                        GamepadView(controller: controller)
                    }
                }
                .frame(width: geo.size.width * CGFloat(gameFraction))
                .clipped()

                if !collapsed {
                    TrackerPanel(state: controller.trackerState)
                        .frame(width: geo.size.width * CGFloat(1 - gameFraction))
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .background(Color.black)
            .overlay(alignment: .topLeading) { topButtons }
            .fileImporter(isPresented: Binding(get: { importKind != nil },
                                               set: { if !$0 { importKind = nil } }),
                          allowedContentTypes: [.data], allowsMultipleSelection: false) { result in
                if case .success(let urls) = result, let url = urls.first {
                    switch importKind {
                    case .rom: controller.loadROM(url: url)
                    case .log: importLog(url)
                    case .none: break
                    }
                }
            }
            .fullScreenCover(isPresented: $showLogViewer) {
                if let d = logData, let active = activeState {
                    LogViewerOverlay(data: d, active: active)
                }
            }
            .sheet(isPresented: $showNaming) { NamingOverlay(controller: controller) }
            .sheet(isPresented: $showSettings, onDismiss: { controller.applyBaselineSpeed() }) { SettingsView() }
            .alert("Error", isPresented: Binding(
                get: { controller.errorMessage != nil },
                set: { if !$0 { controller.errorMessage = nil } })) {
                Button("OK", role: .cancel) { controller.errorMessage = nil }
            } message: {
                Text(controller.errorMessage ?? "")
            }
        }
        .ignoresSafeArea()
    }

    private var topButtons: some View {
        HStack(spacing: 2) {
            Button { onExit() } label: { Image(systemName: "chevron.left.circle.fill").padding(6) }
            if let q = onQuickload {
                Button { q(false) } label: { Image(systemName: "backward.end.fill").padding(6) }
                Button { q(true) } label: { Image(systemName: "forward.end.fill").padding(6) }
            }
            Button { controller.toggleFastForward() } label: {
                Image(systemName: controller.fastForward ? "forward.fill" : "forward")
                    .foregroundColor(controller.fastForward ? .yellow : .white)
                    .padding(6)
            }
            Button { showNaming = true } label: { Image(systemName: "keyboard").padding(6) }
            Button { importKind = .log } label: { Image(systemName: "book").padding(6) }
            Button { showSettings = true } label: { Image(systemName: "gearshape").padding(6) }
            Button { importKind = .rom } label: { Image(systemName: "arrow.triangle.2.circlepath").padding(6) }
            if trackerCollapsible {
                Button { trackerCollapsed.toggle() } label: {
                    Image(systemName: trackerCollapsed ? "sidebar.right" : "arrow.right.to.line").padding(6)
                }
            }
        }
        .font(.system(size: 15))
        .foregroundColor(.white)
        .background(Color.black.opacity(0.35), in: Capsule())
        .padding(6)
    }
}
