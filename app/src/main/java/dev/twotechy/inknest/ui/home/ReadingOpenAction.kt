package dev.twotechy.inknest.ui.home

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
import dev.twotechy.inknest.platform.OpenResult
import dev.twotechy.inknest.platform.ReadingItemOpener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun rememberReadingOpenAction(
    state: HomeUiState,
    opener: ReadingItemOpener?,
    onSettings: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<OpenResult?>(null) }
    var opening by remember { mutableStateOf(false) }
    var request by remember { mutableStateOf<Job?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        request?.cancel()
        error = null
    }
    if (error != null) {
        val message = when (error) {
            OpenResult.MissingApp -> R.string.reading_open_missing_app
            OpenResult.MissingFile -> R.string.reading_open_missing_file
            OpenResult.PermissionLost -> R.string.reading_open_permission
            else -> R.string.reading_open_unavailable
        }
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.reading_open_error)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = {
                    error = null
                    onSettings()
                }) {
                    Text(stringResource(R.string.reading_source_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { error = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    return {
        val loaded = state as? HomeUiState.Loaded
        val current = (loaded?.reading as? ReadingSectionState.Content)?.current
        if (opener == null || current == null) {
            onSettings()
        } else if (!opening) {
            request = scope.launch {
                opening = true
                try {
                    val result = opener.open(current.id)
                    if (result != OpenResult.Success) error = result
                } finally {
                    opening = false
                }
            }
        }
    }
}
