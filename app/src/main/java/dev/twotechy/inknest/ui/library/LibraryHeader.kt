package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.SearchField

@Composable
internal fun LibraryHeader(state: LibraryUiState, actions: LibraryActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.options.searching) {
        if (state.options.searching) focus.requestFocus()
    }
    if (state.options.searching) {
        SearchField(
            state.options.query,
            stringResource(R.string.library_search_hint),
            focus,
            actions.query,
            onClose = { actions.searching(false) },
        )
    } else {
        ScreenHeader(stringResource(R.string.library_title)) {
            HeaderIconButton(R.drawable.ic_search, stringResource(R.string.library_search)) {
                actions.searching(true)
            }
            val grid = state.options.viewMode == LibraryViewMode.Grid
            HeaderIconButton(
                if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                stringResource(if (grid) R.string.switch_to_list else R.string.switch_to_grid),
                actions.toggleView,
            )
            if (state.folders.size > 1) LibraryFolderMenu(state, actions.folder)
            LibrarySortMenu(state.options.sort, actions.sort, actions.refresh)
        }
    }
}

@Composable
private fun LibrarySortMenu(
    selected: LibrarySort,
    onSort: (LibrarySort) -> Unit,
    refresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HeaderIconButton(R.drawable.ic_library_sort, stringResource(R.string.library_sort)) {
            expanded = true
        }
        if (expanded) {
            ContextMenu(onDismiss = { expanded = false }) {
                LibrarySort.entries.forEach { sort ->
                    val label = stringResource(sort.label())
                    ContextMenuItem(
                        if (sort ==
                            selected
                        ) {
                            stringResource(R.string.library_selected_option, label)
                        } else {
                            label
                        },
                    ) {
                        expanded = false
                        onSort(sort)
                    }
                }
                ContextMenuItem(stringResource(R.string.library_refresh)) {
                    expanded = false
                    refresh()
                }
            }
        }
    }
}

private fun LibrarySort.label(): Int = when (this) {
    LibrarySort.Recent -> R.string.library_sort_recent
    LibrarySort.Title -> R.string.library_sort_title
    LibrarySort.Progress -> R.string.library_sort_progress
}
