import SwiftUI
import UIKit

enum Theme {
    // Rósa Parks brand palette — sRGB conversions of the project's OKLCH design
    // tokens. Keep values in sync with the Android
    // side (app/…/ui/theme/Color.kt), which is the first port of these tokens.
    // Dark values invert the brand: midnight blue derived from BrandDarkBlue as
    // ground, cream as type; red and gold stay themselves.

    // Brand constants (identical in both appearances)
    static let brandRed = Color(red: 0xF9/255, green: 0x1A/255, blue: 0x1E/255)
    static let brandGold = Color(red: 0xED/255, green: 0xA3/255, blue: 0x3B/255)
    static let brandDarkBlue = Color(red: 0x0A/255, green: 0x3E/255, blue: 0x77/255)

    // Semantic, appearance-adaptive tokens
    // brandRed only reaches ≈3.8:1 contrast on either ground — keep it for fills
    // and large text; accentText carries red at body size.
    static let background = adaptive(light: 0xECE3DB, dark: 0x081426)
    static let surface = adaptive(light: 0xFDF7F2, dark: 0x10263F)
    static let surfaceSelected = adaptive(light: 0xFBE7DA, dark: 0x1A3152)
    static let textPrimary = adaptive(light: 0x0A3E77, dark: 0xFDF7F2)
    static let textSecondary = adaptive(light: 0x476C96, dark: 0xCFC5BA)
    static let accentText = adaptive(light: 0xC0161A, dark: 0xFF7A6E)
    static let outline = adaptive(light: 0xE2D3C2, dark: 0x24364F)
    static let fillDisabled = adaptive(light: 0xE2D3C2, dark: 0x24364F)
    static let positive = adaptive(light: 0x2E7D32, dark: 0x66BB6A)
    static let negative = adaptive(light: 0xC0161A, dark: 0xFF8A80)

    // Reykjavik parking zone color codes — semantic, not brand. Match the city's
    // signage (Material 500-weights); lifted one step in dark for contrast.
    static let zoneRed = adaptive(light: 0xE53935, dark: 0xEF5350)
    static let zoneBlue = adaptive(light: 0x1E88E5, dark: 0x42A5F5)
    static let zoneGreen = adaptive(light: 0x43A047, dark: 0x66BB6A)
    static let zoneYellow = adaptive(light: 0xFDD835, dark: 0xFDD835)

    static func zoneColor(for id: String) -> Color {
        switch id {
        case "P1": return zoneRed
        case "P2": return zoneBlue
        case "P3": return zoneGreen
        case "P4": return zoneYellow
        default: return .gray
        }
    }

    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light)
        })
    }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
