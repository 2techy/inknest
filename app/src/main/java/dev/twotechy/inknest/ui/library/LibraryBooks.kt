package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.ui.theme.InkLibraryTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryBooks(
    state: LibraryUiState,
    actions: LibraryActions,
    header: @Composable () -> Unit,
) {
    val flatGrid = rememberLazyGridState()
    val flatList = rememberLazyListState()
    val authorGrid = rememberLazyGridState()
    val authorList = rememberLazyListState()
    val index = rememberLazyListState()
    val grid = if (state.options.authorKey == null) flatGrid else authorGrid
    val list = if (state.options.authorKey == null) flatList else authorList
    val criteria =
        listOf(
            state.options.query,
            state.options.filter,
            state.options.sort,
            state.options.folderUri,
            state.options.browse,
        )
    var previous by remember { mutableStateOf(criteria) }
    LaunchedEffect(criteria) {
        // Preserve restored scroll on first composition; reset only for a new user query/order.
        if (previous != criteria) {
            flatGrid.scrollToItem(0)
            flatList.scrollToItem(0)
            authorGrid.scrollToItem(0)
            authorList.scrollToItem(0)
            index.scrollToItem(0)
            previous = criteria
        }
    }
    var previousAuthor by remember { mutableStateOf(state.options.authorKey) }
    LaunchedEffect(state.options.authorKey) {
        if (state.options.authorKey != null && previousAuthor != state.options.authorKey) {
            authorGrid.scrollToItem(0)
            authorList.scrollToItem(0)
        }
        previousAuthor = state.options.authorKey
    }
    if (state.authorIndex) {
        LazyColumn(
            state = index,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            item(key = "controls") { header() }
            items(state.authors, key = LibraryAuthor::key) {
                LibraryAuthorRow(it) { actions.author(it) }
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val gap = InkSpacing.lg
        val width = InkLibraryTokens.minCellWidth * fontScale
        val columns = ((maxWidth + gap) / (width + gap)).toInt().coerceAtLeast(1)
        if (state.options.viewMode == LibraryViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = grid,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
            ) {
                item(key = "controls", span = {
                    GridItemSpan(maxLineSpan)
                }, contentType = "controls") {
                    header()
                }
                items(state.books, key = ReadingItem::libraryKey, contentType = { "book" }) {
                    LibraryGridBook(it, { actions.open(it) }, { actions.details(it) })
                }
            }
        } else {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
            ) {
                item(key = "controls", contentType = "controls") { header() }
                items(state.books, key = ReadingItem::libraryKey, contentType = { "book" }) {
                    LibraryListBook(it, { actions.open(it) }, { actions.details(it) })
                }
            }
        }
    }
}

private fun ReadingItem.libraryKey() = id.sourceId + ":" + id.path
