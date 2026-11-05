package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ChoiceTab
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.components.readingIssueMessage
import dev.twotechy.inknest.ui.settings.sourceFolderLabel
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryScreen(
    state: LibraryUiState,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    ScreenScaffold(
        modifier,
        contentPadding,
        gesturesEnabled = !state.options.searching,
        refresh = RefreshAction(
            !state.notConfigured && !state.loading && !state.options.searching,
            state.isRefreshing,
            actions.refresh,
        ),
        header = { LibraryHeader(state, actions) },
    ) {
        when {
            state.notConfigured -> LibraryStatePanel(
                R.string.library_connect,
                R.string.library_connect_detail,
                actions.settings,
            )

            state.loading -> LibraryStatePanel(
                R.string.library_loading,
                R.string.library_loading_detail,
            )

            state.totalCount == 0 && state.error != null -> Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
            ) {
                StatePanel(
                    stringResource(R.string.library_unavailable),
                    readingIssueMessage(state.error),
                    stringResource(R.string.library_refresh),
                    actions.refresh,
                )
                ActionButton(
                    stringResource(R.string.reading_source_action),
                    onClick = actions.settings,
                )
            }

            else -> LibraryBody(state, actions)
        }
    }
}

@Composable
private fun LibraryBody(state: LibraryUiState, actions: LibraryActions) {
    LibraryBooks(state, actions) {
        Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.md)) {
            LibraryFilters(state.options.filter, state.totalCount, actions.filter)
            state.options.folderUri?.let {
                Text(sourceFolderLabel(it), style = MaterialTheme.typography.bodySmall)
            }
            if (!state.sync.running && state.sync.error != null) {
                state.folders.filter { it.sync.error != null }.forEach { folder ->
                    Text(
                        stringResource(
                            R.string.library_named_folder_error,
                            sourceFolderLabel(folder.uri),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.sync.skippedHistoryEntries > 0) {
                Text(
                    stringResource(R.string.library_skipped, state.sync.skippedHistoryEntries),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.books.isEmpty()) LibraryEmptyState(state.totalCount > 0, actions)
        }
    }
}

@Composable
private fun LibraryEmptyState(filtered: Boolean, actions: LibraryActions) {
    val title = if (filtered) R.string.library_no_matches else R.string.library_empty
    val detail = if (filtered) R.string.library_no_matches_detail else R.string.library_empty_detail
    val label = if (filtered) R.string.library_clear_filters else R.string.library_refresh
    StatePanel(
        stringResource(title),
        stringResource(detail),
        stringResource(label),
        if (filtered) actions.clearFilters else actions.refresh,
    )
}

@Composable
private fun LibraryFilters(selected: LibraryFilter, total: Int, onFilter: (LibraryFilter) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        LibraryFilter.entries.forEach { filter ->
            val label = if (filter == LibraryFilter.All) {
                stringResource(R.string.library_all_count, total)
            } else {
                stringResource(filter.label())
            }
            ChoiceTab(label, selected == filter, { onFilter(filter) })
        }
    }
}

private fun LibraryFilter.label(): Int = when (this) {
    LibraryFilter.All -> R.string.library_filter_all
    LibraryFilter.Reading -> R.string.library_filter_reading
    LibraryFilter.Finished -> R.string.library_filter_finished
    LibraryFilter.Unknown -> R.string.library_filter_unknown
}

@Composable
private fun LibraryStatePanel(title: Int, detail: Int, settings: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StatePanel(
            stringResource(title),
            stringResource(detail),
            settings?.let { stringResource(R.string.reading_source_action) },
            settings ?: {},
        )
    }
}
