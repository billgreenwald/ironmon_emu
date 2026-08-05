import SwiftUI
import UIKit

@main
struct IronmonTrackerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
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
