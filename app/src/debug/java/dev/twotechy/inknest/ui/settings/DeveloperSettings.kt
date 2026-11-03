package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.preview.HomePreviewMode
import dev.twotechy.inknest.data.preview.SourcePreviewController
import dev.twotechy.inknest.data.repository.SourceStatus
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun DeveloperSettings(
    mode: HomePreviewMode?,
    onMode: (HomePreviewMode) -> Unit,
    sources: SourcePreviewController?,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val expansion = stringResource(if (expanded) R.string.hide_tools else R.string.show_tools)
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.md)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget)
                .semantics { stateDescription = expansion }.quietClick { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.developer_tools),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(expansion, style = MaterialTheme.typography.labelMedium)
        }
        if (!expanded) return@Column
        Text(
            stringResource(R.string.preview_explanation),
            style = MaterialTheme.typography.bodySmall,
        )
        mode?.let {
            Text(stringResource(R.string.home_preview))
            HomePreviewMode.entries.forEach { choice ->
                SettingsChoice(choice.name, choice == mode) { onMode(choice) }
            }
        }
        sources?.let {
            Text(stringResource(R.string.source_preview))
            SourceStatus.entries.forEach { status ->
                SettingsAction(sourceStatusLabel(status)) { sources.selectStatus(status) }
            }
            SettingsAction(stringResource(R.string.fail_next_source_action)) {
                sources.failNextAction()
            }
        }
    }
}

@Composable
internal fun DeveloperSettingsRoute(
    homeViewModel: HomeViewModel,
    sources: SourcePreviewController?,
) {
    val mode by homeViewModel.previewMode.collectAsStateWithLifecycle()
    DeveloperSettings(mode, homeViewModel::onPreviewModeChanged, sources)
}
