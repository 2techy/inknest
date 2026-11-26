package dev.twotechy.inknest.ui.components

import androidx.compose.ui.graphics.luminance
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.inkColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayModeColorsTest {
    @Test fun bothModesKeepReadableTextAndSelectionContrast() {
        DisplayMode.entries.forEach { mode ->
            val colors = inkColorScheme(mode)
            val pairs = listOf(
                colors.onBackground to colors.background,
                colors.onSurface to colors.surface,
                colors.onSurfaceVariant to colors.background,
                colors.onPrimary to colors.primary,
                colors.primary to colors.background,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondary to colors.secondary,
            )
            pairs.forEach { (foreground, background) ->
                val ratio = (maxOf(foreground.luminance(), background.luminance()) + .05f) /
                    (minOf(foreground.luminance(), background.luminance()) + .05f)
                assertTrue("$mode contrast $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test fun colorModeKeepsNeutralSurfacesAndErrors() {
        val grayscale = inkColorScheme(DisplayMode.Grayscale)
        val color = inkColorScheme(DisplayMode.Color)
        assertEquals(grayscale.background, color.background)
        assertEquals(grayscale.surface, color.surface)
        assertEquals(grayscale.surfaceVariant, color.surfaceVariant)
        assertEquals(grayscale.primaryContainer, color.primaryContainer)
        assertEquals(grayscale.tertiaryContainer, color.tertiaryContainer)
        assertEquals(color.onBackground, color.onSurface)
        assertEquals(grayscale.error, color.error)
    }
}
