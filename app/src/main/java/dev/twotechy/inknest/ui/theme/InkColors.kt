package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.twotechy.inknest.model.DisplayMode

internal fun inkColorScheme(mode: DisplayMode) = when (mode) {
    DisplayMode.Grayscale -> grayscaleColorScheme
    DisplayMode.Color -> colorfulColorScheme
}

private val grayscaleColorScheme = lightColorScheme(
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
    outlineVariant = InkPalette.muted,
    scrim = InkPalette.primary,
    surfaceBright = InkPalette.background, surfaceDim = InkPalette.surface,
    surfaceContainerLowest = InkPalette.background, surfaceContainerLow = InkPalette.background,
    surfaceContainer = InkPalette.surface, surfaceContainerHigh = InkPalette.surface,
    surfaceContainerHighest = InkPalette.surface,
)

private val colorfulColorScheme = grayscaleColorScheme.copy(
    primary = InkColorPalette.blue,
    onPrimaryContainer = InkColorPalette.ink,
    onSecondaryContainer = InkColorPalette.ink,
    tertiary = InkColorPalette.blue,
    onTertiaryContainer = InkColorPalette.ink,
    onBackground = InkColorPalette.ink,
    onSurface = InkColorPalette.ink,
    onSurfaceVariant = InkColorPalette.muted,
    outline = InkColorPalette.outline, outlineVariant = InkColorPalette.outline,
)
