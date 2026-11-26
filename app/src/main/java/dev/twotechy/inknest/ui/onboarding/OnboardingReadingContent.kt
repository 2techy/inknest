package dev.twotechy.inknest.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.ui.settings.BookFoldersContent
import dev.twotechy.inknest.ui.settings.SettingsDescriptionFormat
import dev.twotechy.inknest.ui.settings.SettingsRow
import dev.twotechy.inknest.ui.settings.SettingsSection
import dev.twotechy.inknest.ui.settings.folderDescription
import dev.twotechy.inknest.ui.shared.SourceFolderActions
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun OnboardingReadingContent(state: OnboardingUiState, actions: SourceFolderActions) {
    val source = state.connection.source
    val folder = source.koreaderFolder
    val folderTitle = if (folder ==
        null
    ) {
        R.string.setup_choose_koreader_folder
    } else {
        R.string.setup_reader_folder
    }
    val detectionLabel = when (state.koreaderDetection) {
        KoreaderDetection.Checking -> R.string.setup_koreader_checking
        KoreaderDetection.Found -> R.string.setup_koreader_found
        KoreaderDetection.NotFound -> R.string.setup_koreader_not_found
        KoreaderDetection.Failed -> R.string.setup_koreader_check_failed
    }
    SettingsSection(stringResource(R.string.setup_koreader_title), contentSpacing = InkSpacing.xs) {
        Text(
            stringResource(detectionLabel),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!source.available) {
            Text(
                stringResource(R.string.integration_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SettingsRow(
            title = stringResource(folderTitle),
            description = folder?.let { folderDescription(it, source.koreaderAccess) },
            enabled = source.available && !state.saving,
            descriptionFormat = SettingsDescriptionFormat.FullSummary,
            onClick = { actions.selectFolder(SourceFolder.KOReader) },
        )
    }
    BookFoldersContent(
        state.connection,
        actions,
        compact = true,
        status = state.bookFolderStatus,
    )
}
