package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection

internal data class SystemGestureEdges(val left: Int, val right: Int) {
    fun contains(x: Float, width: Int): Boolean = x in left.toFloat()..(width - right).toFloat()
}

@Composable
internal fun systemGestureEdges(): SystemGestureEdges {
    val density = LocalDensity.current
    val insets = WindowInsets.systemGestures
    return SystemGestureEdges(
        insets.getLeft(density, LayoutDirection.Ltr),
        insets.getRight(density, LayoutDirection.Ltr),
    )
}
