package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Home identity uses system serif; other launcher text uses system sans-serif.
internal object InkTextStyles {
    val homeBrand = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )
    val homeClock = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 60.sp,
        lineHeight = 68.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )
    val displayClock = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    val pageTitle = uiStyle(22, 28, FontWeight.SemiBold)
    val subPageTitle = uiStyle(20, 26, FontWeight.SemiBold)
    val cardTitle = uiStyle(18, 24, FontWeight.SemiBold)
    val sectionTitle = uiStyle(16, 22, FontWeight.SemiBold)
    val bodyLarge = uiStyle(16, 24)
    val bodyMedium = uiStyle(14, 20)
    val bodySmall = uiStyle(12, 16)
    val labelLarge = uiStyle(14, 20, FontWeight.Medium)
    val labelMedium = uiStyle(12, 16, FontWeight.Medium)
    val labelSmall = uiStyle(11, 16)

    private fun uiStyle(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = weight,
            letterSpacing = 0.sp,
        )
}

internal val InkTypography = Typography(
    displayLarge = InkTextStyles.displayClock,
    displayMedium = InkTextStyles.displayClock,
    displaySmall = InkTextStyles.displayClock,
    headlineLarge = InkTextStyles.pageTitle,
    headlineMedium = InkTextStyles.subPageTitle,
    headlineSmall = InkTextStyles.cardTitle,
    titleLarge = InkTextStyles.cardTitle,
    titleMedium = InkTextStyles.sectionTitle,
    titleSmall = InkTextStyles.sectionTitle,
    bodyLarge = InkTextStyles.bodyLarge,
    bodyMedium = InkTextStyles.bodyMedium,
    bodySmall = InkTextStyles.bodySmall,
    labelLarge = InkTextStyles.labelLarge,
    labelMedium = InkTextStyles.labelMedium,
    labelSmall = InkTextStyles.labelSmall,
)
