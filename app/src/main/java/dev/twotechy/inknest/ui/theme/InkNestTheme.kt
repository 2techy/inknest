package dev.twotechy.inknest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode

internal val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }
internal val LocalDisplayMode = staticCompositionLocalOf { DisplayMode.EInk }

@Composable
fun InkNestTheme(preferences: AppPreferences = AppPreferences(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppPreferences provides preferences,
        LocalDisplayMode provides preferences.displayMode,
    ) {
        MaterialTheme(
            colorScheme = inkColorScheme(preferences.displayMode),
            typography = InkTypography,
            shapes = InkShapes,
            content = content,
        )
    }
}
