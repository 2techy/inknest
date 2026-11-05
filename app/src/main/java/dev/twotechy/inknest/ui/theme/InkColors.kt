package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.twotechy.inknest.model.DisplayMode

internal object InkPalette {
    val primary = Color(0xFF181818)
    val secondary = Color(0xFF333333)
    val muted = Color(0xFF777777)
    val outline = Color(0xFFA0A0A0)
    val surface = Color(0xFFF2F2F2)
    val background = Color.White
}

internal object InkHeatmapPalette {
    val emptyOutline = Color(0xFFB0B0B0)
    val fills = listOf(
        Color(0xFFF5F5F5),
        Color(0xFFD0D0D0),
        Color(0xFFA0A0A0),
        Color(0xFF606060),
        InkPalette.primary,
    )
}

// E-Ink gives structural borders more contrast; small text never uses the light outline gray.
internal fun inkColorScheme(mode: DisplayMode) = lightColorScheme(
    primary = InkPalette.primary, onPrimary = InkPalette.background,
    primaryContainer = InkPalette.surface, onPrimaryContainer = InkPalette.primary,
    inversePrimary = InkPalette.background,
    secondary = InkPalette.secondary, onSecondary = InkPalette.background,
    secondaryContainer = InkPalette.surface, onSecondaryContainer = InkPalette.primary,
    tertiary = InkPalette.secondary, onTertiary = InkPalette.background,
    tertiaryContainer = InkPalette.surface, onTertiaryContainer = InkPalette.primary,
    background = InkPalette.background, onBackground = InkPalette.primary,
    surface = InkPalette.background, onSurface = InkPalette.primary,
    surfaceVariant = InkPalette.surface, onSurfaceVariant = InkPalette.secondary,
    surfaceTint = Color.Transparent,
    inverseSurface = InkPalette.primary, inverseOnSurface = InkPalette.background,
    error = InkPalette.primary, onError = InkPalette.background,
    errorContainer = InkPalette.surface, onErrorContainer = InkPalette.primary,
    outline = InkPalette.muted,
    outlineVariant = if (mode == DisplayMode.EInk) InkPalette.muted else InkPalette.outline,
    scrim = InkPalette.primary,
    surfaceBright = InkPalette.background, surfaceDim = InkPalette.surface,
    surfaceContainerLowest = InkPalette.background, surfaceContainerLow = InkPalette.background,
    surfaceContainer = InkPalette.surface, surfaceContainerHigh = InkPalette.surface,
    surfaceContainerHighest = InkPalette.surface,
)
