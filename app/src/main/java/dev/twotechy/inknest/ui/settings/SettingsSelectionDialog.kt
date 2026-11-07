package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.BlockContentGestures
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun SettingsSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    BlockContentGestures()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(InkBorder.default, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(InkSpacing.lg)) {
                Text(
                    title,
                    modifier = Modifier.semantics { heading() },
                    style = InkTextStyles.subPageTitle,
                )
                content()
                ActionButton(stringResource(R.string.cancel), onClick = onDismiss)
            }
        }
    }
}
