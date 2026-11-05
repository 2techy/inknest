package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.twotechy.inknest.ui.theme.InkReadingTokens

@Composable
internal fun BookCoverPlaceholder(modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.clearAndSetSemantics {}) {
        val width = minOf(size.width * .5f, size.height * .4f)
        val height = width * 1.4f
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2
        val stroke = InkReadingTokens.artworkStroke.toPx()
        drawRoundRect(
            ink,
            Offset(left, top),
            Size(width, height),
            CornerRadius(width * .08f),
            style = Stroke(stroke),
        )
        drawLine(
            ink,
            Offset(left + width * .2f, top),
            Offset(left + width * .2f, top + height),
            stroke,
        )
        drawLine(
            ink,
            Offset(left + width * .2f, top + height * .86f),
            Offset(left + width, top + height * .86f),
            stroke,
        )
    }
}
