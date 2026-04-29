package `is`.rosaparks.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
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

@Composable
fun RosaParksTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            SideEffect {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
