package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.icons.AppIconLoader
import dev.twotechy.inknest.ui.components.AppIcon
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.theme.InkAppGridTokens
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppItem(
    app: AppItemUiModel,
    mode: AppsViewMode,
    icons: AppIconLoader?,
    actions: AppItemActions,
) {
    var menuExpanded by remember(app.componentId) { mutableStateOf(false) }
    val pinned = stringResource(R.string.pinned)
    val modifier = Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget).semantics {
        if (app.isPinned) stateDescription = pinned
    }.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Button,
        onClick = { actions.launch(app.componentId) },
        onLongClickLabel = stringResource(R.string.app_actions),
        onLongClick = { menuExpanded = true },
    ).padding(InkAppGridTokens.cellPadding)
    Box {
        if (mode == AppsViewMode.Grid) {
            GridAppItem(app, icons, modifier)
        } else {
            ListAppItem(app, icons, modifier)
        }
        if (menuExpanded) {
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { menuExpanded = false }
            AppItemMenu(
                app = app,
                onDismiss = { menuExpanded = false },
                actions = actions,
            )
        }
    }
}

@Composable
private fun GridAppItem(app: AppItemUiModel, icons: AppIconLoader?, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkAppGridTokens.iconLabelGap),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconFrame(app, icons, AppsViewMode.Grid)
            if (app.isPinned) PinIcon(Modifier.align(Alignment.TopEnd))
        }
        Text(
            app.label,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ListAppItem(app: AppItemUiModel, icons: AppIconLoader?, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconFrame(app, icons, AppsViewMode.List)
        Spacer(Modifier.width(InkSpacing.md))
        Text(
            app.label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (app.isPinned) {
            Spacer(Modifier.width(InkSpacing.sm))
            PinIcon()
        }
    }
}

@Composable
private fun AppItemMenu(app: AppItemUiModel, onDismiss: () -> Unit, actions: AppItemActions) {
    ContextMenu(onDismiss = onDismiss) {
        ContextMenuItem(
            stringResource(if (app.isPinned) R.string.unpin_app else R.string.pin_app),
        ) {
            onDismiss()
            actions.togglePin(app.componentId, app.isPinned)
        }
        if (app.isPinned) {
            ContextMenuItem(
                stringResource(R.string.move_earlier),
                enabled = app.canMoveEarlier,
            ) {
                onDismiss()
                actions.movePin(app.componentId, -1)
            }
            ContextMenuItem(
                stringResource(R.string.move_later),
                enabled = app.canMoveLater,
            ) {
                onDismiss()
                actions.movePin(app.componentId, 1)
            }
        }
        ContextMenuItem(stringResource(R.string.app_info)) {
            onDismiss()
            actions.openInfo(app.componentId)
        }
    }
}

@Composable
private fun PinIcon(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_app_pin),
        contentDescription = null,
        modifier = modifier.size(InkAppGridTokens.pinIconSize),
    )
}

@Composable
private fun IconFrame(app: AppItemUiModel, icons: AppIconLoader?, mode: AppsViewMode) {
    val grid = mode == AppsViewMode.Grid
    Box(
        Modifier.size(
            if (grid) InkAppGridTokens.iconFrame else InkAppGridTokens.listIconFrame,
        ).clearAndSetSemantics {}.border(
            InkBorder.default,
            if (app.isPinned) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            RoundedCornerShape(InkRadius.medium),
        ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            app.componentId,
            app.label,
            app.iconVersion,
            icons,
            Modifier.size(if (grid) InkAppGridTokens.iconSize else InkAppGridTokens.listIconSize),
        )
    }
}
