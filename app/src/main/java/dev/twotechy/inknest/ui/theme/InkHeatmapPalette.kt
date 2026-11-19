package dev.twotechy.inknest.ui.theme

import androidx.compose.ui.graphics.Color

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
