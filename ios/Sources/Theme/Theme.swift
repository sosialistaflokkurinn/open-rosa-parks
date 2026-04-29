import SwiftUI

enum Theme {
    // Primary — Deep communist red
    static let red900 = Color(red: 0xB7/255, green: 0x1C/255, blue: 0x1C/255)
    static let red800 = Color(red: 0xC6/255, green: 0x28/255, blue: 0x28/255)
    static let red700 = Color(red: 0xD3/255, green: 0x2F/255, blue: 0x2F/255)
    static let red100 = Color(red: 0xFF/255, green: 0xCD/255, blue: 0xD2/255)
    static let red50 = Color(red: 0xFF/255, green: 0xEB/255, blue: 0xEE/255)

    // Secondary — Gold accent
    static let gold = Color(red: 0xFF/255, green: 0xD6/255, blue: 0x00/255)
    static let goldDark = Color(red: 0xC7/255, green: 0xA5/255, blue: 0x00/255)

    // Neutrals
    static let gray50 = Color(red: 0xFA/255, green: 0xFA/255, blue: 0xFA/255)
    static let gray100 = Color(red: 0xF5/255, green: 0xF5/255, blue: 0xF5/255)
    static let gray200 = Color(red: 0xEE/255, green: 0xEE/255, blue: 0xEE/255)
    static let gray600 = Color(red: 0x75/255, green: 0x75/255, blue: 0x75/255)
    static let gray900 = Color(red: 0x21/255, green: 0x21/255, blue: 0x21/255)

    // Zone colors
    static let zoneRed = Color(red: 0xE5/255, green: 0x39/255, blue: 0x35/255)
    static let zoneBlue = Color(red: 0x1E/255, green: 0x88/255, blue: 0xE5/255)
    static let zoneGreen = Color(red: 0x43/255, green: 0xA0/255, blue: 0x47/255)
    static let zoneYellow = Color(red: 0xFD/255, green: 0xD8/255, blue: 0x35/255)

    static func zoneColor(for id: String) -> Color {
        switch id {
        case "P1": return zoneRed
        case "P2": return zoneBlue
        case "P3": return zoneGreen
        case "P4": return zoneYellow
        default: return .gray
        }
    }
}
