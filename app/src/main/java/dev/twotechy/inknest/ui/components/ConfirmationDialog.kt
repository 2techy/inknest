package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BlockContentGestures()
    val shape = RoundedCornerShape(InkRadius.large)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(
            InkBorder.default,
            MaterialTheme.colorScheme.outlineVariant,
            shape,
        ),

        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(title, style = InkTextStyles.subPageTitle) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { ActionButton(confirmLabel, primary = true, onClick = onConfirm) },
        dismissButton = { ActionButton(stringResource(R.string.cancel), onClick = onDismiss) },
    )
}
