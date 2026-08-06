import SwiftUI
import UIKit

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

    var body: some View {
        if controller.romLoaded {
            EmulatorView(controller: controller, onExit: { controller.closeROM() })
        } else {
            LibraryView(
                library: library,
                onPlay: { group in
                    if let path = library.prepareROM(for: group) { controller.openROM(path: path) }
                },
                onPlayFile: { url in controller.loadROM(url: url) }
            )
        }
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
