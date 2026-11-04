package dev.twotechy.inknest.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val model = rememberSourceProbeViewModel(container)
    val visible by model.visible.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    DisposableEffect(model, activity) {
        onDispose {
            if (activity?.isChangingConfigurations == true) model.cancelProbe() else model.close()
        }
    }
    SettingsCommand(stringResource(R.string.reading_source_probe), onClick = model::open)
    if (!visible) return
    val state by model.uiState.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    SourceFolderPicker(pending, model::select, model::dismissFolder)
    Dialog(
        onDismissRequest = model::close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            SourceProbeScreen(
                state,
                { SourceAppProbeRoute(container, model, state) },
                model::close,
                model::recheck,
                model::runProbe,
                model::cancelProbe,
                model::chooseFolder,
            )
        }
    }
}

@Composable
private fun rememberSourceProbeViewModel(container: AppContainer): SourceProbeViewModel {
    val repository = checkNotNull(container.readingSourceConfigRepository)
    val access = checkNotNull(container.safSourceAccess)
    val reader = checkNotNull(container.readerProbeRepository)
    val factory = remember(repository, access, reader) {
        viewModelFactory {
            initializer {
                SourceProbeViewModel(
                    repository,
                    access,
                    createSavedStateHandle(),
                    reader,
                    container.sourceSettingsRepository,
                )
            }
        }
    }
    return viewModel(key = "reading-source-probe", factory = factory)
}
