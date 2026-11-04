package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.CapabilityResult
import dev.twotechy.inknest.data.repository.ProbeIssue
import dev.twotechy.inknest.data.repository.ProbeRead
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.model.SourceAccessState
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SourceProbeScreen(
    state: SourceProbeUiState,
    sourceControls: @Composable () -> Unit,
    close: () -> Unit,
    recheck: () -> Unit,
    runProbe: () -> Unit,
    cancelProbe: () -> Unit,
    choose: (SourceFolder) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(
            InkSpacing.lg,
        ).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
    ) {
        ScreenHeader(stringResource(R.string.reading_source_probe), close, isDetail = true)
        Text(
            stringResource(R.string.probe_grants_description),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.failed) Text(stringResource(R.string.probe_source_failed))
        SettingsCommand(stringResource(R.string.probe_recheck), !state.busy, recheck)
        sourceControls()
        SourceFolder.entries.forEach { folder ->
            val status = if (folder == SourceFolder.KOReader) state.koreader else state.books
            SettingsValue(
                stringResource(
                    if (folder ==
                        SourceFolder.KOReader
                    ) {
                        R.string.koreader_folder
                    } else {
                        R.string.books_folder
                    },
                ),
                stringResource(
                    when (status) {
                        SourceAccessState.NotSelected -> R.string.folder_not_selected
                        SourceAccessState.Granted -> R.string.probe_granted
                        SourceAccessState.PermissionLost -> R.string.source_permission_lost
                        SourceAccessState.InvalidFolder -> R.string.probe_invalid_folder
                        SourceAccessState.Unsupported -> R.string.probe_unsupported
                        SourceAccessState.ProviderError -> R.string.probe_provider_error
                    },
                ),
            )
            SettingsCommand(stringResource(R.string.connect_folder), !state.busy) { choose(folder) }
        }
        SettingsCommand(
            stringResource(R.string.probe_read_lua),
            !state.busy && !state.probing,
            runProbe,
        )
        if (state.probing) SettingsCommand(stringResource(R.string.cancel), onClick = cancelProbe)
        state.reader?.let { result ->
            SettingsValue(
                stringResource(R.string.probe_current),
                when (val item = result.current.result) {
                    is ProbeRead.Success -> stringResource(
                        if (item.value ==
                            null
                        ) {
                            R.string.probe_empty_current
                        } else {
                            R.string.probe_current_found
                        },
                    )

                    is ProbeRead.Unavailable -> probeIssueLabel(item.issue)

                    is ProbeRead.Failed -> probeIssueLabel(item.issue)

                    ProbeRead.NotTested -> stringResource(R.string.probe_not_tested)
                },
            )
            SettingsValue(
                stringResource(R.string.probe_history),
                when (val item = result.history.result) {
                    is ProbeRead.Success -> stringResource(
                        R.string.probe_history_count,
                        item.value.size,
                    )

                    is ProbeRead.Unavailable -> probeIssueLabel(item.issue)

                    is ProbeRead.Failed -> probeIssueLabel(item.issue)

                    ProbeRead.NotTested -> stringResource(R.string.probe_not_tested)
                },
            )
            ProbeCapability(
                stringResource(R.string.probe_resolve_current),
                result.resolvedCurrent,
            ) {
                stringResource(
                    if (it ==
                        null
                    ) {
                        R.string.probe_empty_current
                    } else {
                        R.string.probe_resolved
                    },
                )
            }
            result.resolvedHistory.forEachIndexed { index, item ->
                ProbeCapability(stringResource(R.string.probe_resolve_history, index + 1), item) {
                    stringResource(R.string.probe_resolved)
                }
            }
            ProbeCapability(stringResource(R.string.probe_sidecar), result.sidecar) {
                if (it == null) {
                    stringResource(R.string.probe_empty_current)
                } else {
                    stringResource(
                        R.string.probe_sidecar_value,
                        it.percent?.let { value -> (value * 100).toInt().toString() } ?: "—",
                        it.status ?: "—",
                    )
                }
            }
            ProbeCapability(stringResource(R.string.probe_epub), result.epub) {
                if (it == null) {
                    stringResource(R.string.probe_empty_current)
                } else {
                    stringResource(
                        R.string.probe_epub_value,
                        it.title ?: "—",
                        it.author ?: "—",
                        it.cover.toString(),
                    )
                }
            }
            ProbeCapability(stringResource(R.string.probe_statistics), result.statistics) {
                stringResource(
                    R.string.probe_statistics_value,
                    it.books,
                    it.pageRows,
                    it.walPresent.toString(),
                )
            }
            Text(
                stringResource(R.string.probe_snapshot_limit),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun probeIssueLabel(issue: ProbeIssue): String = stringResource(
    when (issue) {
        ProbeIssue.NotConfigured -> R.string.folder_not_selected
        ProbeIssue.MissingFile -> R.string.probe_missing_file
        ProbeIssue.PermissionLost -> R.string.source_permission_lost
        ProbeIssue.InvalidSnapshot -> R.string.probe_invalid_snapshot
        ProbeIssue.SourceChanged -> R.string.probe_source_changed
        ProbeIssue.InvalidMetadata -> R.string.probe_invalid_metadata
        ProbeIssue.InvalidLua -> R.string.probe_invalid_lua
        ProbeIssue.TooLarge -> R.string.probe_too_large
        ProbeIssue.BudgetExceeded -> R.string.probe_budget_exceeded
        ProbeIssue.ProviderError -> R.string.probe_provider_error
        ProbeIssue.UnresolvedPath -> R.string.probe_outside_tree
        ProbeIssue.Unsupported -> R.string.probe_unsupported_format
        ProbeIssue.UnsupportedSchema -> R.string.probe_unsupported_schema
    },
)

@Composable
private fun <T> ProbeCapability(
    title: String,
    capability: CapabilityResult<T>,
    value: @Composable (T) -> String,
) {
    val detail = when (val result = capability.result) {
        is ProbeRead.Success -> value(result.value)

        is ProbeRead.Unavailable -> stringResource(
            R.string.probe_unavailable_value,
            probeIssueLabel(result.issue),
        )

        is ProbeRead.Failed -> stringResource(
            R.string.probe_failed_value,
            probeIssueLabel(result.issue),
        )

        ProbeRead.NotTested -> stringResource(R.string.probe_not_tested)
    }
    val checked = if (capability.checkedAt >
        0
    ) {
        java.text.DateFormat.getTimeInstance().format(java.util.Date(capability.checkedAt))
    } else {
        "—"
    }
    SettingsValue(title, stringResource(R.string.probe_checked_value, detail, checked))
}
