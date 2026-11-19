package dev.twotechy.inknest.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.platform.reading.ReadingItemDispatcher
import dev.twotechy.inknest.ui.components.ReadingOpenEffect
import dev.twotechy.inknest.ui.components.ReadingOpenErrorActions
import dev.twotechy.inknest.ui.library.details.BookDetailsActions
import dev.twotechy.inknest.ui.library.details.BookDetailsScreen
import dev.twotechy.inknest.ui.library.details.BookDetailsViewModel

@Composable
internal fun LibraryRoute(
    viewModel: LibraryViewModel,
    detailsViewModel: BookDetailsViewModel,
    dispatcher: ReadingItemDispatcher,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val open = viewModel.bookOpening.actions.open
    ReadingOpenEffect(
        state.opening,
        viewModel.bookOpening.actions,
        dispatcher,
        ReadingOpenErrorActions(onSettings, viewModel::refresh),
    )
    BackHandler(state.options.authorKey != null) { viewModel.backToAuthors() }
    BackHandler(state.options.searching) { viewModel.searching(false) }
    val selected by viewModel.selectedBook.collectAsStateWithLifecycle()
    val selectedId = selected?.takeIf {
        it.size == 2
    }?.let { ReadingItemId(it[0], it[1]) }
    LaunchedEffect(selectedId) { detailsViewModel.select(selectedId) }
    BackHandler(selectedId != null) { viewModel.closeDetails() }
    if (selectedId != null) {
        val details by detailsViewModel.uiState.collectAsStateWithLifecycle()
        key(selectedId) {
            BookDetailsScreen(
                details,
                BookDetailsActions(
                    viewModel::closeDetails,
                    { open(selectedId) },
                    detailsViewModel::toggleDescription,
                    detailsViewModel::toggleHistory,
                    detailsViewModel::toggleFile,
                    detailsViewModel::historyPage,
                    detailsViewModel::retry,
                ),
                modifier,
                contentPadding,
            )
        }
    }
    val shelves = rememberSaveableStateHolder()
    if (selectedId == null) {
        shelves.SaveableStateProvider("shelf") {
            LibraryScreen(
                state,
                LibraryActions(
                    viewModel::query, viewModel::searching, viewModel::filter, viewModel::sort,
                    viewModel::toggleView, viewModel::clearFilters, viewModel::refresh, onSettings,
                    open = viewModel::details,
                    details = viewModel::details,
                    folder = viewModel::folder,
                    browse = viewModel::browse,
                    author = viewModel::author,
                    backToAuthors = viewModel::backToAuthors,
                ),
                modifier,
                contentPadding,
            )
        }
    }
}
