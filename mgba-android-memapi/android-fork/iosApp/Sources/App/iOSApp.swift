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
    @StateObject private var pads = GameControllerManager()
    @State private var activeGroup: RomFamilyGroup?

    var body: some View {
        Group {
            if controller.romLoaded {
                EmulatorView(
                    controller: controller,
                    pads: pads,
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
        // Library home is portrait (like Android); only the emulator is forced landscape.
        .onChange(of: controller.romLoaded) { loaded in
            OrientationLocker.lock(loaded ? .landscape : .portrait,
                                   rotateTo: loaded ? .landscapeRight : .portrait)
        }
        .onAppear {
            OrientationLocker.lock(controller.romLoaded ? .landscape : .portrait,
                                   rotateTo: controller.romLoaded ? .landscapeRight : .portrait)
            pads.attach(
                forward: { mask in Task { @MainActor in controller.setPadKeys(mask) } },
                onSpeed: { on in Task { @MainActor in controller.speedTrigger(pressed: on) } }
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

/// The Info.plist orientation keys alone weren't locking the app on device, so we hard-enforce it
/// here. The lock is dynamic: the library home is portrait (Android parity) and the emulator (a
/// landscape split: game left / tracker right) forces landscape. `OrientationLocker` flips the mask
/// and asks the active window scene to re-evaluate the current orientation.
final class AppDelegate: NSObject, UIApplicationDelegate {
    static var orientationLock: UIInterfaceOrientationMask = .portrait
    func application(_ application: UIApplication,
                     supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        AppDelegate.orientationLock
    }
}

enum OrientationLocker {
    static func lock(_ mask: UIInterfaceOrientationMask, rotateTo: UIInterfaceOrientation) {
        AppDelegate.orientationLock = mask
        if #available(iOS 16.0, *) {
            for scene in UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }) {
                scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask))
                scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
            }
        } else {
            UIDevice.current.setValue(rotateTo.rawValue, forKey: "orientation")
            UIViewController.attemptRotationToDeviceOrientation()
        }
    }
}
