package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkChoiceTokens
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.LocalDisplayMode

// The visible tab stays compact; its full row height remains available for touch.
@Composable
internal fun ChoiceTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val colorMode = LocalDisplayMode.current == DisplayMode.Color
    Box(
        modifier.sizeIn(
            minWidth = InkHeaderTokens.touchTarget,
            minHeight = InkHeaderTokens.touchTarget,
        )
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().heightIn(min = InkChoiceTokens.minHeight),
            shape = RoundedCornerShape(InkRadius.medium),
            color = when {
                !selected -> colors.surface
                colorMode -> colors.primaryContainer
                else -> colors.primary
            },
            border = if (selected &&
                colorMode
            ) {
                BorderStroke(InkBorder.default, colors.primary)
            } else {
                null
            },
            contentColor = when {
                !selected -> colors.onSurface
                colorMode -> colors.onPrimaryContainer
                else -> colors.onPrimary
            },
        ) {
            Text(
                label,
                Modifier.padding(
                    horizontal = InkChoiceTokens.horizontalPadding,
                    vertical = InkChoiceTokens.verticalPadding,
                ),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// Equal-width segments share Stats' compact outline and scroll at large fonts/narrow widths.
@Composable
internal fun ChoiceTabGroup(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    if (labels.isEmpty()) return
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val labelWidth = labels.maxOf { measurer.measure(it, style, maxLines = 1).size.width }
    val minimumWidth = with(density) { labelWidth.toDp() } + InkChoiceTokens.horizontalPadding * 2
    val visibleHeight = maxOf(
        InkChoiceTokens.minHeight,
        with(density) { style.lineHeight.toDp() } + InkChoiceTokens.verticalPadding * 2,
    )
    val outline = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints {
        val width = maxOf(maxWidth / labels.size, InkHeaderTokens.touchTarget, minimumWidth)
        val scroll = rememberScrollState()
        Row(
            Modifier.selectableGroup().drawWithContent {
                drawContent()
                val height = visibleHeight.toPx()
                val top = (size.height - height) / 2
                val stroke = InkBorder.thin.toPx()
                repeat(labels.size - 1) { index ->
                    val offset = width.toPx() * (index + 1) - scroll.value
                    val x = if (rtl) size.width - offset else offset
                    if (x > 0 && x < size.width) {
                        drawLine(outline, Offset(x, top), Offset(x, top + height), stroke)
                    }
                }
                drawRoundRect(
                    outline,
                    Offset(stroke / 2, top + stroke / 2),
                    Size(size.width - stroke, height - stroke),
                    CornerRadius(InkRadius.medium.toPx()),
                    style = Stroke(stroke),
                )
            }.horizontalScroll(scroll),
        ) {
            labels.forEachIndexed { index, label ->
                ChoiceTab(label, selectedIndex == index, { onSelect(index) }, Modifier.width(width))
            }
        }
    }
}
