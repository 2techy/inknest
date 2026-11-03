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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.AppIcon

@Composable
internal fun AppItem(
    app: AppItemUiModel,
    mode: AppsViewMode,
    icons: AppIconLoader?,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onMovePin: (Int) -> Unit,
) {
    var menuExpanded by remember(app.componentId) { mutableStateOf(false) }
    val modifier = Modifier.fillMaxWidth().combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
        onLongClick = { menuExpanded = true },
    ).padding(6.dp)
    Box {
        if (mode == AppsViewMode.Grid) {
            Column(
                modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconFrame(app, icons, mode)
                Text(
                    app.label,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                IconFrame(app, icons, mode)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        app.componentId.substringBefore('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (app.isPinned) {
                    Text(
                        stringResource(R.string.pinned),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (app.isPinned) R.string.unpin_app else R.string.pin_app))
                },
                onClick = {
                    menuExpanded = false
                    onTogglePin()
                },
            )
            if (app.isPinned) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_earlier)) },
                    enabled = app.canMoveEarlier,
                    onClick = {
                        menuExpanded = false
                        onMovePin(-1)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_later)) },
                    enabled = app.canMoveLater,
                    onClick = {
                        menuExpanded = false
                        onMovePin(1)
                    },
                )
            }
        }
    }
}

@Composable
private fun IconFrame(app: AppItemUiModel, icons: AppIconLoader?, mode: AppsViewMode) {
    val grid = mode == AppsViewMode.Grid
    Box(
        Modifier.size(if (grid) 48.dp else 38.dp).border(
            1.25.dp,
            if (app.isPinned) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            RoundedCornerShape(if (grid) 10.dp else 8.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            app.componentId,
            app.label,
            app.iconVersion,
            icons,
            Modifier.size(if (grid) 36.dp else 28.dp),
        )
    }
}
