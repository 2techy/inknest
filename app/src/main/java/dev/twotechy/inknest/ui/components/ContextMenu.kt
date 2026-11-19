package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkMenuTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

// Static popup: unlike Material DropdownMenu it has no scale/fade transition or shadow.
@Composable
internal fun ContextMenu(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignToEnd: Boolean = false,
    content: @Composable () -> Unit,
) {
    BlockContentGestures()
    Popup(
        popupPositionProvider = remember(alignToEnd) { ContextMenuPosition(alignToEnd) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = modifier.widthIn(
                max = InkMenuTokens.maxWidth,
            ).width(InkMenuTokens.defaultWidth),
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
internal fun ContextMenuItem(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = ContextMenuEntryRow(ContextMenuEntry(label, enabled), modifier, onClick)

@Composable
internal fun ContextMenuEntryRow(
    entry: ContextMenuEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget).semantics {
            entry.selected?.let { this.selected = it }
        }.clickable(
            enabled = entry.enabled,
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ).padding(
            horizontal = InkMenuTokens.horizontalPadding,
            vertical = InkMenuTokens.verticalPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(InkMenuTokens.iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (entry.enabled) colors.onSurface else colors.outline
        entry.leadingIcon?.let {
            Icon(painterResource(it), null, Modifier.size(InkMenuTokens.iconSize), tint = tint)
        }
        Text(
            entry.label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
        val trailingIcon =
            entry.trailingIcon ?: R.drawable.ic_check.takeIf { entry.selected == true }
        if (trailingIcon != null) {
            Icon(
                painterResource(trailingIcon),
                null,
                Modifier.size(InkMenuTokens.iconSize),
                tint = tint,
            )
        } else if (entry.selected != null) {
            Spacer(Modifier.size(InkMenuTokens.iconSize))
        }
    }
}

@Composable
internal fun ContextMenuHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = InkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            R.drawable.ic_back,
            stringResource(R.string.back),
            placement = HeaderIconPlacement.Centered,
            onClick = onBack,
        )
        Text(
            title,
            Modifier.weight(
                1f,
            ).padding(end = InkSpacing.md, top = InkSpacing.sm, bottom = InkSpacing.sm),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    HorizontalDivider()
}

private class ContextMenuPosition(private val alignToEnd: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val alignRight = (layoutDirection == LayoutDirection.Ltr) == alignToEnd
        val desiredX = if (alignRight) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
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
