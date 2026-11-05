package dev.twotechy.inknest.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.platform.OpenResult
import dev.twotechy.inknest.platform.ReadingItemOpener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun rememberReadingOpenAction(
    opener: ReadingItemOpener?,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
): (ReadingItemId) -> Unit {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<OpenResult?>(null) }
    var opening by remember { mutableStateOf(false) }
    var request by remember { mutableStateOf<Job?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        request?.cancel()
        error = null
    }
    error?.let { issue ->
        ReadingOpenErrorDialog(issue, { error = null }, onSettings, onRefresh)
    }
    return { id ->
        if (opener == null) {
            onSettings()
        } else if (!opening) {
            request = scope.launch {
                opening = true
                try {
                    val result = opener.open(id)
                    if (result != OpenResult.Dispatched) error = result
                } finally {
                    opening = false
                }
            }
        }
    }
}

@Composable
private fun ReadingOpenErrorDialog(
    error: OpenResult,
    dismiss: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val message = when (error) {
        OpenResult.MissingApp -> R.string.reading_open_missing_app
        OpenResult.Unsupported -> R.string.reading_open_unsupported
        OpenResult.MissingFile -> R.string.reading_open_missing_file
        OpenResult.PermissionLost -> R.string.reading_open_permission
        else -> R.string.reading_open_unavailable
    }
    BlockContentGestures()
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.reading_open_error)) },
        text = { Text(stringResource(message)) },
        confirmButton = {
            if (error != OpenResult.Unsupported) {
                val missingFile = error == OpenResult.MissingFile
                TextButton(onClick = {
                    dismiss()
                    if (missingFile) onRefresh() else onSettings()
                }) {
                    val label = if (missingFile) {
                        R.string.reading_open_refresh
                    } else {
                        R.string.reading_source_action
                    }
                    Text(stringResource(label))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
