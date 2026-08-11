package `is`.rosaparks.ui.theme

import androidx.compose.ui.graphics.Color

// Rósa Parks brand palette — sRGB conversions of the project's OKLCH design
// tokens. Convert via Chrome's canvas (oklch() →
// fillStyle → getImageData) so wide-gamut clipping matches the live site.
val BrandRed = Color(0xFFF91A1E) // --primary   oklch(0.6239 0.2458 27.91)
val BrandGold = Color(0xFFEDA33B) // --secondary oklch(0.7704 0.144 71.22)
val BrandDarkBlue = Color(0xFF0A3E77) // --fg     oklch(0.3664 0.1104 254.9)

// Three cream shades, light → dark
val BrandCreamOverlay = Color(0xFFFDF7F2) // --overlay  oklch(0.98 0.01 64.44) — cards, popovers
val BrandCream = Color(0xFFFDE9D7) // --bg            oklch(0.9453 0.0324 64.44) — page bg
val BrandCreamMuted = Color(0xFFECE3DB) // --muted     oklch(0.92 0.015 64.44) — dim surface

// Dark theme: the brand inverted — midnight blue derived from BrandDarkBlue as
// ground, cream as type. Red and gold stay themselves. Values shared with the
// iOS port (ios/Sources/Theme/Theme.swift).
val BrandMidnight = Color(0xFF081426) // dark page bg
val BrandMidnightElevated = Color(0xFF10263F) // dark cards, popovers
val BrandMidnightBorder = Color(0xFF24364F) // dark outlines, dividers
val BrandCreamDim = Color(0xFFCFC5BA) // secondary text on midnight

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val GreyLight = Color(0xFFF5F5F5)
val GreyMedium = Color(0xFFBDBDBD)

// Reykjavik parking zone color codes — semantic, not brand. Match the city's signage.
val ZoneRed = Color(0xFFE53935)
val ZoneBlue = Color(0xFF1E88E5)
val ZoneGreen = Color(0xFF43A047)
val ZoneYellow = Color(0xFFFDD835)
