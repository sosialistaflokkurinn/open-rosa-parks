package `is`.rosaparks.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import `is`.rosaparks.R

private val GothicA1 =
    FontFamily(
        Font(R.font.gothica1_500, weight = FontWeight.Normal),
        Font(R.font.gothica1_900, weight = FontWeight.Black),
    )

private val Perfektta =
    FontFamily(
        Font(R.font.perfektta_extrabold, weight = FontWeight.ExtraBold),
    )

private val baseTypography = Typography()

val Typography =
    baseTypography.copy(
        // Display + headline use Perfektta — fyrirsagnir og stórt letur
        displayLarge = baseTypography.displayLarge.copy(fontFamily = Perfektta),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = Perfektta),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = Perfektta),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = Perfektta),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = Perfektta),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = Perfektta),
        // Title + body + label use GothicA1
        titleLarge = baseTypography.titleLarge.copy(fontFamily = GothicA1),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = GothicA1),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = GothicA1),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = GothicA1),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = GothicA1),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = GothicA1),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = GothicA1),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = GothicA1),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = GothicA1),
    )
