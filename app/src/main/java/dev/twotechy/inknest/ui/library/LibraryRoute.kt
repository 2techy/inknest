package dev.twotechy.inknest.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.platform.ReadingItemDispatcher
import dev.twotechy.inknest.ui.components.ReadingOpenEffect
import dev.twotechy.inknest.ui.components.ReadingOpenErrorActions

@Composable
internal fun LibraryRoute(
    viewModel: LibraryViewModel,
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
    var selected by remember { mutableStateOf<ReadingItem?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { selected = null }
    selected?.let { book ->
        LibraryBookDialog(book, onDismiss = { selected = null }, onOpen = {
            selected = null
            open(book.id)
        })
    }
    LibraryScreen(
        state,
        LibraryActions(
            viewModel::query, viewModel::searching, viewModel::filter, viewModel::sort,
            viewModel::toggleView, viewModel::clearFilters, viewModel::refresh, onSettings,
            open = { open(it.id) },
            details = { selected = it },
            folder = viewModel::folder,
            browse = viewModel::browse,
            author = viewModel::author,
            backToAuthors = viewModel::backToAuthors,
        ),
        modifier,
        contentPadding,
    )
}
