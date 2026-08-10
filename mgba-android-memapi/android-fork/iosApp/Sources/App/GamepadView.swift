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

/// On-screen GBA controls, drawn as a transparent OVERLAY over the game (not a bar beneath it):
/// shoulders top, D-pad + A/B at the bottom corners, Start/Select bottom-center. Opacity, scale, and
/// left/right inversion are user-configurable (Settings).
struct GamepadView: View {
    @ObservedObject var controller: EmulatorController
    @AppStorage("controls_alpha") private var alpha = 0.5
    @AppStorage("controls_scale") private var scale = 1.0
    @AppStorage("invert_layout") private var invert = false

    private func key(_ b: GBAButton, _ shape: some View) -> some View {
        HoldButton(onDown: { controller.press(b) }, onUp: { controller.release(b) }) { shape }
    }
    private func round(_ text: String, _ s: CGFloat) -> some View {
        Text(text).font(.system(size: 20 * s, weight: .bold))
            .frame(width: 52 * s, height: 52 * s)
            .background(Circle().fill(Color.gray.opacity(0.55)))
            .foregroundColor(.white)
    }
    private func dpad(_ text: String, _ s: CGFloat) -> some View {
        Text(text).font(.system(size: 18 * s))
            .frame(width: 44 * s, height: 44 * s)
            .background(RoundedRectangle(cornerRadius: 6).fill(Color.gray.opacity(0.55)))
            .foregroundColor(.white)
    }
    private func shoulder(_ text: String, _ s: CGFloat) -> some View {
        Text(text).font(.system(size: 14 * s))
            .frame(width: 66 * s, height: 34 * s)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.55)))
            .foregroundColor(.white)
    }

    private func dpadCluster(_ s: CGFloat) -> some View {
        VStack(spacing: 2) {
            key(.up, dpad("▲", s))
            HStack(spacing: 2) { key(.left, dpad("◀", s)); Spacer().frame(width: 44 * s); key(.right, dpad("▶", s)) }
            key(.down, dpad("▼", s))
        }
    }
    private func abCluster(_ s: CGFloat) -> some View {
        HStack(spacing: 14 * s) {
            key(.b, round("B", s))
            key(.a, round("A", s))
        }
    }

    var body: some View {
        let s = CGFloat(scale)
        ZStack {
            // Shoulders — top corners
            VStack {
                HStack { key(.l, shoulder("L", s)); Spacer(); key(.r, shoulder("R", s)) }
                Spacer()
            }
            // Bottom row: D-pad and A/B at the corners, Start/Select centered
            VStack {
                Spacer()
                HStack(alignment: .bottom) {
                    (invert ? AnyView(abCluster(s)) : AnyView(dpadCluster(s)))
                    Spacer()
                    HStack(spacing: 12 * s) { key(.select, shoulder("Sel", s)); key(.start, shoulder("Start", s)) }
                    Spacer()
                    (invert ? AnyView(dpadCluster(s)) : AnyView(abCluster(s)))
                }
                .padding(.bottom, 10)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .opacity(alpha)
    }
}
