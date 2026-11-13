package dev.twotechy.inknest.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.AppUpdateError
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.model.UpdateDownloadStage
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppUpdatesContent(
    state: AppUpdateState,
    notes: ReleaseNotesDocument,
    actions: UpdateSettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap)) {
        UpdateVersionSection(state, actions)
        Column {
            state.snapshot.release?.let { release ->
                ReleaseNotes(release, notes, actions.openLink)
            }
            SettingsRow(
                stringResource(R.string.updates_view_release),
                onClick = actions.releasePage,
            )
        }
        if (state.automaticAllowed) {
            SettingsSection(stringResource(R.string.settings_general_heading)) {
                SettingsToggle(
                    stringResource(R.string.updates_automatic),
                    stringResource(R.string.updates_automatic_description),
                    state.snapshot.automatic,
                    state.loaded,
                    actions.automatic,
                )
            }
        }
    }
}

@Composable
private fun UpdateVersionSection(state: AppUpdateState, actions: UpdateSettingsActions) {
    val action = updatePrimaryAction(state)
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        Text(
            updateStatusText(state),
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (state.download == UpdateDownloadStage.Downloading) {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(state.progress.toFloat(), 0f..100f)
                }
            },
            style = MaterialTheme.typography.titleLarge,
        )
        UpdateVersionGrid(state.snapshot.release)
        state.snapshot.checkedAt?.let {
            Text(
                stringResource(R.string.updates_last_checked, updateDate(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsCommand(
            stringResource(action.label),
            enabled = state.loaded && action.enabled,
            onClick = { actions.perform(action) },
        )
    }
}

@Composable
internal fun updateStatusText(state: AppUpdateState): String = updateOperationText(state)
    ?: stringResource(
        when {
            !state.loaded -> R.string.loading_settings

            state.available -> R.string.updates_available

            !state.canCompareVersion && state.snapshot.release != null ->
                R.string.updates_latest_found

            state.snapshot.checkedAt != null -> R.string.updates_current

            else -> R.string.updates_not_checked
        },
    )

@Composable
private fun updateOperationText(state: AppUpdateState): String? = when {
    state.checking -> stringResource(R.string.updates_checking)

    state.error != null -> stringResource(updateErrorResource(state.error))

    state.download == UpdateDownloadStage.Downloading ->
        stringResource(R.string.updates_progress, state.progress)

    state.download == UpdateDownloadStage.Verifying -> stringResource(R.string.updates_verifying)

    state.download == UpdateDownloadStage.Ready -> stringResource(R.string.updates_ready)

    else -> null
}

private fun updateErrorResource(error: AppUpdateError): Int = when (error) {
    AppUpdateError.Check -> R.string.updates_check_failed
    AppUpdateError.Download -> R.string.updates_download_failed
    AppUpdateError.Verify -> R.string.updates_verify_failed
    AppUpdateError.Storage -> R.string.updates_storage_failed
    AppUpdateError.RateLimit -> R.string.updates_rate_limited
}

@Composable
private fun ReleaseNotes(
    release: AppRelease,
    document: ReleaseNotesDocument,
    openLink: (String) -> Unit,
) {
    if (release.notes.isBlank()) return
    var expanded by rememberSaveable(release.version) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        SettingsRow(
            stringResource(R.string.updates_release_notes),
            stringResource(
                if (expanded) R.string.updates_hide_notes else R.string.updates_show_notes,
            ),
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            if (document.source == release.notes) {
                ReleaseNotesContent(document, openLink)
            } else {
                Text(stringResource(R.string.updates_loading_notes))
            }
        }
    }
}

@Composable
private fun updateDate(timestamp: Long): String = DateUtils.formatDateTime(
    LocalContext.current,
    timestamp,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
)
