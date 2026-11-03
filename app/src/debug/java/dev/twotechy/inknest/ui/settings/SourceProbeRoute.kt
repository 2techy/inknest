package dev.twotechy.inknest.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twotechy.inknest.R
import dev.twotechy.inknest.di.AppContainer

@Composable
internal fun ReadingSourceProbeEntry(container: AppContainer) {
    var visible by remember { mutableStateOf(false) }
    SettingsCommand(stringResource(R.string.reading_source_probe)) { visible = true }
    if (!visible) return
    val repository = checkNotNull(container.readingSourceConfigRepository)
    val access = checkNotNull(container.safSourceAccess)
    val reader = checkNotNull(container.readerProbeRepository)
    val factory = remember(repository, access, reader) {
        viewModelFactory {
            initializer {
                SourceProbeViewModel(repository, access, createSavedStateHandle(), reader)
            }
        }
    }
    val model: SourceProbeViewModel = viewModel(key = "reading-source-probe", factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    DisposableEffect(model) { onDispose { model.cancelProbe() } }
    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data
            model.select(
                if (result.resultCode ==
                    Activity.RESULT_OK
                ) {
                    data?.data
                } else {
                    null
                },
                data?.flags ?: 0,
            )
        }
    Dialog(onDismissRequest = {
        visible = false
    }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            SourceProbeScreen(state, {
                visible = false
            }, model::recheck, model::runProbe, model::cancelProbe) { folder ->
                model.pending = folder
                picker.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                        )
                    },
                )
            }
        }
    }
}
