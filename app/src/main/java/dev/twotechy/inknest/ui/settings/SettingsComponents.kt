package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SettingsRow(
    title: String,
    description: String? = null,
    icon: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = InkSettingsTokens.rowMinHeight)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = InkSettingsTokens.rowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            if (icon != null) {
                Box(
                    Modifier.size(InkSettingsTokens.leadingSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), null, Modifier.size(InkSettingsTokens.iconSize))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (enabled) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    null,
                    Modifier.size(InkSettingsTokens.chevronSize),
                )
            }
        }
        HorizontalDivider(
            thickness = InkBorder.thin,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
internal fun SettingsChoice(
    title: String,
    selected: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    onClick: () -> Unit,
) = SettingsSelectionRow(
    title,
    description,
    SelectionState(selected, enabled, Role.RadioButton),
    onClick,
)

@Composable
internal fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingsSelectionRow(title, description, SelectionState(checked, enabled, Role.Checkbox)) {
        onChange(!checked)
    }
    HorizontalDivider(thickness = InkBorder.thin, color = MaterialTheme.colorScheme.outlineVariant)
}

private data class SelectionState(val selected: Boolean, val enabled: Boolean, val role: Role)

@Composable
private fun SettingsSelectionRow(
    title: String,
    description: String?,
    state: SelectionState,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val radio = state.role == Role.RadioButton
    val interactionModifier = if (radio) {
        Modifier.selectable(
            state.selected,
            enabled = state.enabled,
            role = state.role,
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier.toggleable(
            state.selected,
            enabled = state.enabled,
            role = state.role,
            interactionSource = interaction,
            indication = null,
            onValueChange = { onClick() },
        )
    }
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = if (radio) {
                InkSettingsTokens.choiceMinHeight
            } else {
                InkSettingsTokens.toggleMinHeight
            },
        ).then(interactionModifier).padding(vertical = InkSettingsTokens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let { SettingsDescription(it) }
        }
        SelectionMark(state.selected, radio, state.enabled)
    }
}

@Composable
private fun SelectionMark(selected: Boolean, radio: Boolean, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val ink = if (enabled) colors.onSurface else colors.outline
    val paper = MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(InkSettingsTokens.selectionSize)) {
        val stroke = InkSettingsTokens.selectionStroke.toPx()
        if (radio) {
            drawCircle(ink, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
            if (selected) drawCircle(ink, radius = size.minDimension * .23f)
        } else {
            val inset = stroke / 2
            val bounds = Size(size.width - stroke, size.height - stroke)
            drawRoundRect(
                ink,
                Offset(inset, inset),
                bounds,
                CornerRadius(InkRadius.small.toPx()),
                style = if (selected) Fill else Stroke(stroke),
            )
            if (selected) {
                val check = Path().apply {
                    moveTo(size.width * .24f, size.height * .5f)
                    lineTo(size.width * .43f, size.height * .69f)
                    lineTo(size.width * .77f, size.height * .31f)
                }
                drawPath(
                    check,
                    paper,
                    style = Stroke(
                        InkSettingsTokens.checkStroke.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun SettingsAction(title: String, enabled: Boolean = true, onClick: () -> Unit) {
    ActionButton(title, enabled = enabled, onClick = onClick)
}

@Composable
internal fun SettingsErrorPanel(error: SettingsError, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(InkBorder.default, MaterialTheme.colorScheme.outlineVariant)
            .padding(InkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
    ) {
        Text(
            stringResource(
                when (error) {
                    SettingsError.Read -> R.string.preferences_read_error
                    SettingsError.Save -> R.string.preferences_save_error
                    SettingsError.Source -> R.string.source_action_error
                    SettingsError.FolderConflict -> R.string.book_folder_conflict
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        val label = if (error == SettingsError.Read) R.string.retry else R.string.dismiss
        SettingsAction(
            stringResource(label),
            onClick = if (error == SettingsError.Read) onRetry else onDismiss,
        )
    }
}

// Read-only metadata has no chevron or button outline.
@Composable
internal fun SettingsValue(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = InkSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun SettingsCommand(title: String, enabled: Boolean = true, onClick: () -> Unit) {
    ActionButton(
        title,
        modifier = Modifier.fillMaxWidth().padding(vertical = InkSpacing.xs),
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun SettingsDescription(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column { content() }
    }
}
