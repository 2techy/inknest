package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode

internal val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }
internal val LocalDisplayMode = staticCompositionLocalOf { DisplayMode.Grayscale }
internal val LocalHeatmapColors = staticCompositionLocalOf { InkHeatmapPalette.grayscale }

@Composable
fun InkNestTheme(preferences: AppPreferences = AppPreferences(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppPreferences provides preferences,
        LocalDisplayMode provides preferences.displayMode,
        LocalHeatmapColors provides when (preferences.displayMode) {
            DisplayMode.Grayscale -> InkHeatmapPalette.grayscale
            DisplayMode.Color -> InkHeatmapPalette.color
        },
    ) {
        MaterialTheme(
            colorScheme = inkColorScheme(preferences.displayMode),
            typography = InkTypography,
            shapes = InkShapes,
            content = content,
        )
    }
}
