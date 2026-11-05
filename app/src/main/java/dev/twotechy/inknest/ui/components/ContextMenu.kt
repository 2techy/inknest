package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkMenuTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

// Static popup: unlike Material DropdownMenu it has no scale/fade transition or shadow.
@Composable
internal fun ContextMenu(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BlockContentGestures()
    Popup(
        popupPositionProvider = remember { ContextMenuPosition() },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(InkBorder.default, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                Modifier.widthIn(min = InkMenuTokens.minWidth, max = InkMenuTokens.maxWidth)
                    .verticalScroll(rememberScrollState()).padding(vertical = InkSpacing.xs),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun ContextMenuItem(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        label,
        Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget).clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ).padding(horizontal = InkSpacing.lg, vertical = InkSpacing.md),
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) colors.onSurface else colors.outline,
    )
}

private class ContextMenuPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val desiredX = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val desiredY = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) {
            anchorBounds.bottom
        } else {
            anchorBounds.top - popupContentSize.height
        }
        return IntOffset(
            desiredX.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            desiredY.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}
