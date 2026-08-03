import SwiftUI

@main
struct IronmonTrackerApp: App {
    var body: some Scene {
        WindowGroup {
            // Uses MockMemoryProvider by default (shows "Disconnected") until the mGBA core is
            // wired in — swap to MgbaMemoryProvider once the emulator base is integrated.
            ContentView()
        }
    }
}
