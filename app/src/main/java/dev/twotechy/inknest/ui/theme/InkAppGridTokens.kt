package dev.twotechy.inknest.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object InkAppGridTokens {
    const val HOME_MAX_COLUMNS = 4
    val minCellWidth = 84.dp
    val horizontalGap = InkSpacing.md
    val verticalGap = InkSpacing.md
    val cellPadding = InkSpacing.xs
    val iconFrame = 48.dp
    val iconSize = 36.dp
    val listIconFrame = 40.dp
    val listIconSize = 28.dp
    val pinIconSize = InkSpacing.lg
    val iconLabelGap = InkSpacing.sm

    fun columnsFor(availableWidth: Dp, fontScale: Float): Int {
        val cellWidth = minCellWidth * fontScale.coerceAtLeast(1f)
        return ((availableWidth + horizontalGap) / (cellWidth + horizontalGap)).toInt()
            .coerceAtLeast(1)
    }
}
