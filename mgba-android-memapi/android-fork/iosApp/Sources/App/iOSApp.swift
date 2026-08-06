import SwiftUI
import UIKit
import TrackerCore

@main
struct IronmonTrackerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}

/// Routes between the ROM library (home) and the running game. Owns the shared controller + library.
struct RootView: View {
    @StateObject private var controller = EmulatorController()
    @StateObject private var library = RomLibrary()
    @State private var activeGroup: RomFamilyGroup?

    var body: some View {
        if controller.romLoaded {
            EmulatorView(
                controller: controller,
                onExit: { controller.closeROM(); activeGroup = nil },
                onQuickload: activeGroup == nil ? nil : { next in quickload(next: next) }
            )
        } else {
            LibraryView(
                library: library,
                onPlay: { group in
                    activeGroup = group
                    if let path = library.prepareROM(for: group) { controller.openROM(path: path) }
                },
                onPlayFile: { url in activeGroup = nil; controller.loadROM(url: url) }
            )
        }
    }

    /// Quickload: switch to the next/previous ROM in the active family.
    private func quickload(next: Bool) {
        guard let group = activeGroup else { return }
        let numbers = library.memberNumbers(for: group)
        guard !numbers.isEmpty else { return }
        let idx = numbers.firstIndex(of: library.lastPlayed(group.prefix)) ?? 0
        let target = next ? numbers[min(idx + 1, numbers.count - 1)] : numbers[max(idx - 1, 0)]
        if let path = library.prepareROM(for: group, number: target) { controller.openROM(path: path) }
    }
}

/// The Info.plist orientation keys alone weren't locking the app to landscape on device, so we
/// hard-enforce it here. The whole tracker layout is a landscape split (game left / tracker right).
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        .landscape
    }
}
