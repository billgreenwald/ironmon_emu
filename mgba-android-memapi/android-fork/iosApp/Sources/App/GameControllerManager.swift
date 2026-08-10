import Foundation
import GameController

/// A remappable physical input on a hardware gamepad. The D-pad and left thumbstick are hard-wired
/// to the GBA D-pad (not remappable); everything here is a face/shoulder/menu button the user can
/// bind to any GBA button in Settings.
enum PadInput: String, CaseIterable, Identifiable {
    case buttonA, buttonB, buttonX, buttonY
    case leftShoulder, rightShoulder, leftTrigger, rightTrigger
    case menu, options

    var id: String { rawValue }

    /// Label as printed on a typical Xbox-style controller (positions are normalized by GameController).
    var displayName: String {
        switch self {
        case .buttonA: return "A (bottom)"
        case .buttonB: return "B (right)"
        case .buttonX: return "X (left)"
        case .buttonY: return "Y (top)"
        case .leftShoulder: return "L1 / LB"
        case .rightShoulder: return "R1 / RB"
        case .leftTrigger: return "L2 / LT"
        case .rightTrigger: return "R2 / RT"
        case .menu: return "Menu / Start"
        case .options: return "Options / Select"
        }
    }

    /// The default GBA target for this physical input (used until the user remaps it).
    var defaultTarget: GBATarget {
        switch self {
        case .buttonA: return .a
        case .buttonB: return .b
        case .buttonX: return .b
        case .buttonY: return .a
        case .leftShoulder, .leftTrigger: return .l
        case .rightShoulder, .rightTrigger: return .r
        case .menu: return .start
        case .options: return .select
        }
    }
}

/// A bindable target: a GBA button, the special Fast-forward action (Android's speed button), or
/// "unbound". `fastForward` never contributes to the GBA key mask — the controller layer detects it
/// and drives the emulator speed instead.
enum GBATarget: String, CaseIterable, Identifiable {
    case a, b, l, r, start, select, fastForward, none

    var id: String { rawValue }
    var label: String {
        switch self {
        case .a: return "A"; case .b: return "B"; case .l: return "L"; case .r: return "R"
        case .start: return "Start"; case .select: return "Select"
        case .fastForward: return "⏩ Fast-forward"; case .none: return "— Unbound"
        }
    }
    var mask: UInt32 {
        switch self {
        case .a: return GBAButton.a.rawValue
        case .b: return GBAButton.b.rawValue
        case .l: return GBAButton.l.rawValue
        case .r: return GBAButton.r.rawValue
        case .start: return GBAButton.start.rawValue
        case .select: return GBAButton.select.rawValue
        case .fastForward, .none: return 0
        }
    }
}

/// User's physical-input → GBA-button map, persisted in UserDefaults ("bind_<input>"). Falls back to
/// each input's `defaultTarget` when a key is unset, so a fresh install has a sensible mapping.
struct ControllerBindings {
    private static func key(_ i: PadInput) -> String { "bind_\(i.rawValue)" }

    func target(_ i: PadInput) -> GBATarget {
        if let raw = UserDefaults.standard.string(forKey: Self.key(i)),
           let t = GBATarget(rawValue: raw) { return t }
        return i.defaultTarget
    }
    func setTarget(_ t: GBATarget, for i: PadInput) {
        UserDefaults.standard.set(t.rawValue, forKey: Self.key(i))
    }
    static func resetToDefaults() {
        for i in PadInput.allCases { UserDefaults.standard.removeObject(forKey: key(i)) }
    }
}

/// Bridges MFi / Bluetooth controllers (Xbox, DualShock/DualSense, MFi pads) to the emulator.
/// Observes connect/disconnect, installs a `valueChangedHandler` that rebuilds the full GBA key
/// mask on every input event, and forwards it to `EmulatorController.setPadKeys`.
///
/// Deliberately *not* `@MainActor`: `valueChangedHandler` and the GameController notifications are
/// delivered on the main queue in practice, but the compiler sees the handler as nonisolated, so
/// keeping the class nonisolated lets it touch `bindings`/`forward` directly. The `forward` closure
/// hops to the main actor for the actual (main-actor) emulator call.
final class GameControllerManager: ObservableObject {
    @Published private(set) var connectedName: String?

    private var forward: ((UInt32) -> Void)?
    private var onSpeed: ((Bool) -> Void)?
    private var speedHeld = false            // last edge of the Fast-forward binding
    private var bindings = ControllerBindings()

    /// Wire up forwarding + start listening. Call once (e.g. from RootView.onAppear).
    /// `onSpeed` receives the down/up edge of any input bound to Fast-forward.
    func attach(forward: @escaping (UInt32) -> Void, onSpeed: @escaping (Bool) -> Void) {
        self.forward = forward
        self.onSpeed = onSpeed
        NotificationCenter.default.addObserver(
            self, selector: #selector(didConnect), name: .GCControllerDidConnect, object: nil)
        NotificationCenter.default.addObserver(
            self, selector: #selector(didDisconnect), name: .GCControllerDidDisconnect, object: nil)
        // Re-read bindings when Settings changes them (cheap; avoids reading defaults per input event).
        NotificationCenter.default.addObserver(
            self, selector: #selector(reloadBindings), name: UserDefaults.didChangeNotification, object: nil)
        GCController.startWirelessControllerDiscovery { }
        if let c = GCController.controllers().first { bind(c) }
    }

    @objc private func reloadBindings() { bindings = ControllerBindings() }

    @objc private func didConnect(_ note: Notification) {
        if let c = note.object as? GCController { bind(c) }
    }

    @objc private func didDisconnect(_ note: Notification) {
        connectedName = nil
        forward?(0)   // drop any keys the departing pad was holding
        if speedHeld { speedHeld = false; onSpeed?(false) }  // and release fast-forward
    }

    private func bind(_ controller: GCController) {
        connectedName = controller.vendorName ?? "Controller"
        guard let pad = controller.extendedGamepad else { return }
        pad.valueChangedHandler = { [weak self] pad, _ in
            guard let self else { return }
            var mask: UInt32 = 0
            var ff = false
            func add(_ input: PadInput, _ pressed: Bool) {
                guard pressed else { return }
                let target = self.bindings.target(input)
                if target == .fastForward { ff = true }     // speed trigger, not a GBA button
                else { mask |= target.mask }
            }
            add(.buttonA, pad.buttonA.isPressed)
            add(.buttonB, pad.buttonB.isPressed)
            add(.buttonX, pad.buttonX.isPressed)
            add(.buttonY, pad.buttonY.isPressed)
            add(.leftShoulder, pad.leftShoulder.isPressed)
            add(.rightShoulder, pad.rightShoulder.isPressed)
            add(.leftTrigger, pad.leftTrigger.isPressed)
            add(.rightTrigger, pad.rightTrigger.isPressed)
            add(.menu, pad.buttonMenu.isPressed)                 // non-optional since iOS 13
            add(.options, pad.buttonOptions?.isPressed ?? false) // optional; some pads lack it

            // D-pad + left stick → GBA D-pad (fixed).
            if pad.dpad.up.isPressed    || pad.leftThumbstick.up.isPressed    { mask |= GBAButton.up.rawValue }
            if pad.dpad.down.isPressed  || pad.leftThumbstick.down.isPressed  { mask |= GBAButton.down.rawValue }
            if pad.dpad.left.isPressed  || pad.leftThumbstick.left.isPressed  { mask |= GBAButton.left.rawValue }
            if pad.dpad.right.isPressed || pad.leftThumbstick.right.isPressed { mask |= GBAButton.right.rawValue }

            self.forward?(mask)
            if ff != self.speedHeld { self.speedHeld = ff; self.onSpeed?(ff) }  // down/up edge only
        }
    }
}
