package `is`.rosaparks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = BrandRed,
        onPrimary = White,
        primaryContainer = BrandRed,
        onPrimaryContainer = White,
        secondary = BrandGold,
        onSecondary = BrandDarkBlue,
        secondaryContainer = BrandGold,
        onSecondaryContainer = BrandDarkBlue,
        tertiary = BrandDarkBlue,
        onTertiary = BrandCream,
        tertiaryContainer = BrandDarkBlue,
        onTertiaryContainer = BrandCream,
        error = Color(0xFFB00020),
        onError = White,
        errorContainer = Color(0xFFFCD8DF),
        onErrorContainer = Color(0xFF410E0B),
        background = BrandCreamMuted,
        onBackground = BrandDarkBlue,
        surface = BrandCreamMuted,
        onSurface = BrandDarkBlue,
        surfaceVariant = BrandCream,
        onSurfaceVariant = BrandDarkBlue,
        surfaceContainerLowest = BrandCreamOverlay,
        surfaceContainerLow = BrandCreamOverlay,
        surfaceContainer = BrandCreamOverlay,
        surfaceContainerHigh = BrandCreamOverlay,
        surfaceContainerHighest = BrandCreamOverlay,
        surfaceBright = BrandCreamOverlay,
        surfaceDim = BrandCreamMuted,
        inverseSurface = BrandDarkBlue,
        inverseOnSurface = BrandCream,
        outline = GreyMedium,
        outlineVariant = GreyLight,
        scrim = Color(0x99000000),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = BrandRed,
        onPrimary = White,
        primaryContainer = BrandRed,
        onPrimaryContainer = White,
        secondary = BrandGold,
        onSecondary = BrandMidnight,
        secondaryContainer = BrandGold,
        onSecondaryContainer = BrandMidnight,
        tertiary = BrandCreamOverlay,
        onTertiary = BrandMidnight,
        tertiaryContainer = BrandCreamOverlay,
        onTertiaryContainer = BrandMidnight,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = BrandMidnight,
        onBackground = BrandCreamOverlay,
        surface = BrandMidnight,
        onSurface = BrandCreamOverlay,
        surfaceVariant = BrandMidnightElevated,
        onSurfaceVariant = BrandCreamDim,
        surfaceContainerLowest = BrandMidnightElevated,
        surfaceContainerLow = BrandMidnightElevated,
        surfaceContainer = BrandMidnightElevated,
        surfaceContainerHigh = BrandMidnightElevated,
        surfaceContainerHighest = BrandMidnightElevated,
        surfaceBright = BrandMidnightElevated,
        surfaceDim = BrandMidnight,
        inverseSurface = BrandCreamOverlay,
        inverseOnSurface = BrandDarkBlue,
        outline = BrandMidnightBorder,
        outlineVariant = BrandMidnightBorder,
        scrim = Color(0x99000000),
    )

@Composable
fun RosaParksTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            SideEffect {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
