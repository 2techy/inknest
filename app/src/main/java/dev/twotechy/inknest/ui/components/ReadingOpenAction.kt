package dev.twotechy.inknest.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.ReadingOpenState
import dev.twotechy.inknest.platform.ReadingItemDispatcher
import dev.twotechy.inknest.ui.reading.ReadingOpenActions

internal data class ReadingOpenErrorActions(val settings: () -> Unit, val refresh: () -> Unit)

@Composable
internal fun ReadingOpenEffect(
    state: ReadingOpenState,
    actions: ReadingOpenActions,
    dispatcher: ReadingItemDispatcher,
    errors: ReadingOpenErrorActions,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { actions.cancel() }
    DisposableEffect(actions) { onDispose { actions.cancel() } }
    val request = when (state) {
        is ReadingOpenState.Ready -> state.request
        is ReadingOpenState.Dispatching -> state.request
        else -> null
    }
    LaunchedEffect(request?.id, lifecycle, dispatcher) {
        if (request != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                actions.claim(request.id)?.let { prepared ->
                    actions.complete(request.id, dispatcher.dispatch(prepared))
                }
            }
        }
    }
    (state as? ReadingOpenState.Error)?.let {
        ReadingOpenErrorDialog(it.error, actions.dismiss, errors.settings, errors.refresh)
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
