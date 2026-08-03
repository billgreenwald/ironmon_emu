import SwiftUI

/// GBA button identity → its mGBA key bit (1 << GBAKey). Kept in Swift to avoid NS_OPTIONS
/// import-naming ambiguity; values match EmulatorCore.h's GBAKeyMask.
enum GBAButton: UInt32 {
    case a = 1, b = 2, select = 4, start = 8
    case right = 16, left = 32, up = 64, down = 128
    case r = 256, l = 512
}

/// A view that fires onDown when first touched and onUp when released (for hold-to-press controls).
private struct HoldButton<Label: View>: View {
    let onDown: () -> Void
    let onUp: () -> Void
    @ViewBuilder var label: () -> Label
    @State private var pressed = false

    var body: some View {
        label()
            .opacity(pressed ? 0.55 : 1.0)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in if !pressed { pressed = true; onDown() } }
                    .onEnded { _ in pressed = false; onUp() }
            )
    }
}

/// On-screen GBA controls: D-pad, A/B, Start/Select, L/R. Presses drive EmulatorController.
struct GamepadView: View {
    @ObservedObject var controller: EmulatorController

    private func key(_ b: GBAButton, _ shape: some View) -> some View {
        HoldButton(onDown: { controller.press(b) }, onUp: { controller.release(b) }) { shape }
    }

    private func round(_ text: String) -> some View {
        Text(text)
            .font(.headline).frame(width: 52, height: 52)
            .background(Circle().fill(Color.gray.opacity(0.3)))
    }

    private func dpad(_ text: String) -> some View {
        Text(text).font(.headline).frame(width: 44, height: 44)
            .background(RoundedRectangle(cornerRadius: 6).fill(Color.gray.opacity(0.3)))
    }

    private func shoulder(_ text: String) -> some View {
        Text(text).font(.subheadline).frame(width: 70, height: 34)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.3)))
    }

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                key(.l, shoulder("L")); Spacer(); key(.r, shoulder("R"))
            }
            HStack(alignment: .center) {
                // D-pad
                VStack(spacing: 2) {
                    key(.up, dpad("▲"))
                    HStack(spacing: 2) { key(.left, dpad("◀")); Spacer().frame(width: 44); key(.right, dpad("▶")) }
                    key(.down, dpad("▼"))
                }
                Spacer()
                // A/B
                VStack(spacing: 14) {
                    key(.a, round("A"))
                    key(.b, round("B"))
                }
            }
            HStack(spacing: 24) {
                key(.select, shoulder("Select"))
                key(.start, shoulder("Start"))
            }
        }
        .padding(8)
    }
}
