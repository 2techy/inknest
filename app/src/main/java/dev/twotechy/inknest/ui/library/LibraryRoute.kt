package dev.twotechy.inknest.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId

@Composable
internal fun LibraryRoute(
    viewModel: LibraryViewModel,
    onBook: (ReadingItemId) -> Unit,
    onSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val details: (ReadingItem) -> Unit = { onBook(it.id) }
    BackHandler(state.options.authorKey != null) { viewModel.backToAuthors() }
    BackHandler(state.options.searching) { viewModel.searching(false) }
    LibraryScreen(
        state,
        LibraryActions(
            viewModel::query, viewModel::searching, viewModel::filter, viewModel::sort,
            viewModel::toggleView, viewModel::clearFilters, viewModel::refresh, onSettings,
            open = details,
            details = details,
            folder = viewModel::folder,
            browse = viewModel::browse,
            author = viewModel::author,
            backToAuthors = viewModel::backToAuthors,
        ),
        contentPadding = contentPadding,
    )
}
