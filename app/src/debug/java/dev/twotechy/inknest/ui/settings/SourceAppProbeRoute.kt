package dev.twotechy.inknest.ui.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.ProbeRead
import dev.twotechy.inknest.data.repository.ReaderProbeResult
import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.model.ReadingOpenMode
import kotlinx.coroutines.launch

@Composable
internal fun SourceAppProbeRoute(
    container: AppContainer,
    model: SourceProbeViewModel,
    state: SourceProbeUiState,
) {
    val apps by container.installedAppsRepository.installedApps.collectAsStateWithLifecycle(
        emptyList(),
    )
    val candidates = apps.filter { it.componentId.substringBefore('/') == "org.koreader.launcher" }
    val selected = candidates.firstOrNull {
        it.componentId.substringBefore('/') == state.config.sourcePackage
    }
    val context = LocalContext.current
    val launcher = remember(context, container) {
        SourceProbeLauncher(
            context,
            container.appLauncher,
            checkNotNull(container.readerProbeRepository),
        )
    }
    val version =
        remember(selected?.componentId, selected?.iconVersion) { launcher.version(selected) }
    val targets = state.reader.openTargets()
    val keys = targets.map { it.document.uri }
    var confirmedA by remember(keys, state.config.sourcePackage, version) { mutableStateOf(false) }
    var confirmedB by remember(keys, state.config.sourcePackage, version) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reportFailure = {
        Toast.makeText(context, R.string.probe_open_failed, Toast.LENGTH_SHORT).show()
    }
    SourceAppProbeControls(
        state = SourceAppProbeState(
            candidates = candidates,
            selectedComponent = selected?.componentId,
            targets = targets.size,
            enabled = !state.busy && !state.probing,
            versionAvailable = version != null,
            confirmedA = confirmedA,
            confirmedB = confirmedB,
            mode = if (version != null && state.config.verifiedAppVersion == version) {
                state.config.openMode
            } else {
                ReadingOpenMode.Unverified
            },
        ),
        select = model::selectPackage,
        launchApp = { if (!launcher.launchApp(selected)) reportFailure() },
        launchBook = { index ->
            targets.getOrNull(index)?.let { target ->
                scope.launch {
                    if (!launcher.launchBook(state.config, target.sourcePath)) reportFailure()
                }
            }
        },
        confirmA = { confirmedA = it },
        confirmB = { confirmedB = it },
        record = { mode -> version?.let { model.recordOpenMode(mode, it) } },
    )
}

private fun ReaderProbeResult?.openTargets(): List<ResolvedBook> = buildList {
    (this@openTargets?.resolvedCurrent?.result as? ProbeRead.Success)?.value?.let { add(it) }
    this@openTargets?.resolvedHistory.orEmpty().forEach { item ->
        (item.result as? ProbeRead.Success)?.value?.let { add(it) }
    }
}.distinctBy { it.document.uri }.take(2)
