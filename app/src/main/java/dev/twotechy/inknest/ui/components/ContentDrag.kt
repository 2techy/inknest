package dev.twotechy.inknest.ui.components

import dev.twotechy.inknest.ui.theme.InkGestureTokens
import kotlin.math.abs

internal enum class DragDirection { Undecided, Horizontal, Vertical }

// One direction per contact; an ambiguous diagonal never becomes an accidental tab change.
internal class ContentDrag(private val slop: Float) {
    var x = 0f
        private set
    var y = 0f
        private set
    var direction = DragDirection.Undecided
        private set

    fun move(dx: Float, dy: Float) {
        x += dx
        y += dy
        if (direction != DragDirection.Undecided) return
        val ratio = InkGestureTokens.DIRECTION_RATIO
        direction = when {
            abs(x) > slop && abs(x) > abs(y) * ratio -> DragDirection.Horizontal
            abs(y) > slop && abs(y) > abs(x) * ratio -> DragDirection.Vertical
            else -> DragDirection.Undecided
        }
    }

    fun tabOffset(threshold: Float): Int = when {
        direction != DragDirection.Horizontal || abs(x) < threshold -> 0
        x < 0 -> 1
        else -> -1
    }
}
