package dev.twotechy.inknest.ui.theme

import androidx.compose.ui.graphics.Color

internal data class InkHeatmapPalette(val emptyOutline: Color, val fills: List<Color>) {
    companion object {
        val grayscale = InkHeatmapPalette(
            Color(0xFFB0B0B0),
            listOf(
                Color(0xFFF5F5F5),
                Color(0xFFD0D0D0),
                Color(0xFFA0A0A0),
                Color(0xFF606060),
                InkPalette.primary,
            ),
        )
        val color = InkHeatmapPalette(
            grayscale.emptyOutline,
            listOf(
                grayscale.fills.first(),
                Color(0xFFDFE7F0),
                Color(0xFFA9BED4),
                Color(0xFF6888A7),
                InkColorPalette.blue,
            ),
        )
    }
}
