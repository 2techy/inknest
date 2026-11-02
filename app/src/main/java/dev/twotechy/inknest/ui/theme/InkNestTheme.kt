package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val InkColors = lightColorScheme(
    primary = Color(0xFF181818), onPrimary = Color.White,
    background = Color.White, onBackground = Color(0xFF181818),
    surface = Color.White, onSurface = Color(0xFF181818),
    surfaceVariant = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFF777777), outlineVariant = Color(0xFFA0A0A0),
)

private val InkTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun InkNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = InkColors, typography = InkTypography, content = content)
}
