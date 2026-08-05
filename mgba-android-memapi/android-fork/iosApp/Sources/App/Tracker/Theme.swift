import SwiftUI

/// Ported 1:1 from Android `TrackerPanel.kt:54-99` (palette, type colors, move-category split).
enum TrackerTheme {
    static let panelBg      = Color(hex: 0x0F1621)
    static let headerBg     = Color(hex: 0x16213E)
    static let cardBg       = Color(hex: 0x1A2540)
    static let accentRed    = Color(hex: 0xE94560)
    static let accentBlue   = Color(hex: 0x4090FF)
    static let textPrimary  = Color(hex: 0xEEEEEE)
    static let textSecondary = Color(hex: 0xAAAAAA)
    static let hpHigh       = Color(hex: 0x4CAF50)
    static let hpMid        = Color(hex: 0xFFEB3B)
    static let hpLow        = Color(hex: 0xF44336)
    static let statBoost    = Color(hex: 0x4CAF50)
    static let statReduce   = Color(hex: 0xFF6B6B)
    static let star         = Color(hex: 0xFFCC00)
    static let noteYellow   = Color(hex: 0xFFEB3B)

    static let typeColors: [Int: Color] = [
        0: Color(hex: 0xA8A878), 1: Color(hex: 0xC03028), 2: Color(hex: 0x8EB8E0), 3: Color(hex: 0xA040A0),
        4: Color(hex: 0xE0C068), 5: Color(hex: 0xB8A038), 6: Color(hex: 0xA8B820), 7: Color(hex: 0x705898),
        8: Color(hex: 0xB8B8D0), 10: Color(hex: 0xF08030), 11: Color(hex: 0x6890F0), 12: Color(hex: 0x78C850),
        13: Color(hex: 0xF8D030), 14: Color(hex: 0xF85888), 15: Color(hex: 0x98D8D8), 16: Color(hex: 0x7038F8),
        17: Color(hex: 0x705848), 18: Color(hex: 0xEE99AC),
    ]

    static func typeColor(_ id: Int) -> Color { typeColors[id] ?? Color(hex: 0x888888) }
    static func hpColor(_ pct: Float) -> Color { pct > 0.5 ? hpHigh : (pct > 0.2 ? hpMid : hpLow) }

    /// Gen III physical/special split (`TrackerPanel.kt:95-99`): types 0-8 physical, 10-17 special.
    static func typeIsPhysical(_ typeId: Int) -> Bool { typeId <= 8 }
}

extension Color {
    init(hex: UInt32) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: 1)
    }
}
