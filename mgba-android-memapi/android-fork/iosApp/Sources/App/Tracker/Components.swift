import SwiftUI
import TrackerCore

/// Kotlin's nested `TrackerState.Active` is exported to Swift under a flattened name. Alias it once
/// so every view can name the type — and if the interop name differs, this is the only line to fix.
typealias ActiveState = TrackerStateActive

// Reusable tracker UI atoms. All values coming from the shared Kotlin models arrive as Int32 —
// convert to Int at the point of Swift use.

/// Pokémon sprite loaded from the bundled `sprites/` folder (N.png for natDexId≥412, else N.gif —
/// GIFs render as a static first frame for now). Falls back to `#id` when missing.
struct PokemonSprite: View {
    let natDexId: Int
    var size: CGFloat = 48

    var body: some View {
        if let img = SpriteCache.image(natDexId) {
            Image(uiImage: img).interpolation(.none).resizable().scaledToFit()
                .frame(width: size, height: size)
        } else {
            Text("#\(natDexId)")
                .font(.system(size: size * 0.26)).foregroundColor(TrackerTheme.textSecondary)
                .frame(width: size, height: size)
        }
    }
}

enum SpriteCache {
    private static var cache: [Int: UIImage] = [:]
    static func image(_ natDexId: Int) -> UIImage? {
        if let c = cache[natDexId] { return c }
        let ext = natDexId >= 412 ? "png" : "gif"
        guard let url = Bundle.main.url(forResource: "\(natDexId)", withExtension: ext, subdirectory: "sprites"),
              let img = UIImage(contentsOfFile: url.path) else { return nil }
        cache[natDexId] = img
        return img
    }
}

/// HP bar; green >50% / yellow >20% / red, with optional `cur/max` numbers.
struct HpBar: View {
    let percent: Float
    var cur: Int? = nil
    var maxHp: Int? = nil
    var showNumbers: Bool = true

    var body: some View {
        VStack(spacing: 1) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.12))
                    Capsule().fill(TrackerTheme.hpColor(percent))
                        .frame(width: geo.size.width * CGFloat(min(1, Swift.max(0, percent))))
                }
            }
            .frame(height: 6)
            if showNumbers, let c = cur, let m = maxHp {
                HStack {
                    Spacer()
                    Text("\(c)/\(m)").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
                }
            }
        }
    }
}

/// Colored type pill.
struct TypeChip: View {
    let typeId: Int
    var body: some View {
        Text(TypeChart.shared.typeName(typeId: Int32(typeId)).uppercased())
            .font(.system(size: 9, weight: .bold)).foregroundColor(.white)
            .padding(.horizontal, 5).padding(.vertical, 2)
            .background(TrackerTheme.typeColor(typeId), in: Capsule())
    }
}

/// Renders type1 + (type2 if different & valid).
struct TypeChips: View {
    let type1: Int
    let type2: Int
    var body: some View {
        HStack(spacing: 3) {
            if (0...18).contains(type1) { TypeChip(typeId: type1) }
            if (0...18).contains(type2) && type2 != type1 { TypeChip(typeId: type2) }
        }
    }
}

/// Status condition badge (SLP/PSN/BRN/FRZ/PAR/TOX); nothing when healthy.
struct StatusBadge: View {
    let status: Int
    var body: some View {
        if let label = TrackerStateSwift.shared.statusLabel(status: Int32(status)) {
            Text(label)
                .font(.system(size: 8, weight: .bold)).foregroundColor(.white)
                .padding(.horizontal, 4).padding(.vertical, 1)
                .background(color(for: label), in: Capsule())
        }
    }
    private func color(for label: String) -> Color {
        switch label.prefix(3) {
        case "SLP": return Color(hex: 0x8888AA)
        case "PSN", "TOX": return Color(hex: 0xA040A0)
        case "BRN": return Color(hex: 0xF08030)
        case "FRZ": return Color(hex: 0x98D8D8)
        case "PAR": return Color(hex: 0xF8D030)
        default: return .gray
        }
    }
}

/// Move category dot: physical=orange, special=blue, status=pink.
struct CategoryIcon: View {
    let category: Int   // 1=physical, 2=special, 3=status
    var body: some View {
        Image(systemName: category == 3 ? "circle.grid.cross.fill" : (category == 1 ? "burst.fill" : "diamond.fill"))
            .font(.system(size: 8))
            .foregroundColor(category == 1 ? Color(hex: 0xF08030) : (category == 2 ? Color(hex: 0x6890F0) : Color(hex: 0xF85888)))
    }
}

/// Type-effectiveness arrow vs a defender (Android `TrackerPanel.kt` Eff column).
struct EffectivenessArrow: View {
    let mult: Float
    var body: some View {
        let (sym, col): (String, Color) = {
            switch mult {
            case 0:            return ("✕", Color(hex: 0x888888))
            case 0.25:         return ("⇊", Color(hex: 0xF44336))
            case 0.5:          return ("↓", Color(hex: 0xFF9800))
            case 2:            return ("↑", Color(hex: 0x8BC34A))
            case 4:            return ("⇈", Color(hex: 0x4CAF50))
            default:           return ("", .clear)
            }
        }()
        Text(sym).font(.system(size: 10, weight: .bold)).foregroundColor(col)
    }
}

/// Inline gender / shiny / pokérus markers next to a name.
struct MonMarkers: View {
    let genderSymbol: String
    let isShiny: Bool
    let hasPokerus: Bool
    var body: some View {
        HStack(spacing: 2) {
            if !genderSymbol.isEmpty {
                Text(genderSymbol)
                    .foregroundColor(genderSymbol == "♂" ? TrackerTheme.accentBlue : TrackerTheme.accentRed)
            }
            if isShiny { Text("✦").foregroundColor(TrackerTheme.star) }
            if hasPokerus { Text("✚").foregroundColor(TrackerTheme.hpHigh) }
        }
        .font(.system(size: 11))
    }
}

/// A labeled value row used across sections.
struct InfoRow: View {
    let label: String
    let value: String
    var valueColor: Color = TrackerTheme.textPrimary
    var body: some View {
        HStack {
            Text(label).font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary)
            Spacer()
            Text(value).font(.system(size: 11)).foregroundColor(valueColor)
        }
    }
}

/// Card container matching Android CardBg.
struct TrackerCard<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 5) { content }
            .padding(8)
            .background(TrackerTheme.cardBg, in: RoundedRectangle(cornerRadius: 8))
    }
}
