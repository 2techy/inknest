package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.settings.sourceFolderLabel
import dev.twotechy.inknest.ui.theme.InkSpacing

private enum class LibraryMenuPage(val title: Int) {
    Root(R.string.library_options),
    Browse(R.string.library_menu_browse),
    Folders(R.string.book_folders),
    Layout(R.string.library_menu_layout),
    Sort(R.string.library_menu_sort),
}
private data class LibraryMenuChoice(
    val label: String,
    val selected: Boolean,
    val action: () -> Unit,
)

@Composable
internal fun LibraryOptionsMenu(state: LibraryUiState, actions: LibraryActions) {
    var page by remember { mutableStateOf<LibraryMenuPage?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { page = null }
    Box {
        HeaderIconButton(R.drawable.ic_more, stringResource(R.string.library_options)) {
            page =
                LibraryMenuPage.Root
        }
        page?.let { current ->
            ContextMenu(onDismiss = { page = null }) {
                if (current == LibraryMenuPage.Root) {
                    LibraryMenuSections(state) { page = it }
                    ContextMenuItem(
                        stringResource(R.string.library_refresh),
                        enabled =
                            !state.notConfigured && !state.isRefreshing,
                    ) {
                        page = null
                        actions.refresh()
                    }
                } else {
                    LibrarySubmenuHeader(current) { page = LibraryMenuPage.Root }
                    LibraryMenuChoices(current, state, actions).forEach { choice ->
                        ContextMenuItem(
                            if (choice.selected) {
                                stringResource(
                                    R.string.library_selected_option,
                                    choice.label,
                                )
                            } else {
                                choice.label
                            },
                        ) {
                            page = null
                            choice.action()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySubmenuHeader(page: LibraryMenuPage, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(end = InkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(R.drawable.ic_back, stringResource(R.string.back), onBack)
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryMenuSections(state: LibraryUiState, select: (LibraryMenuPage) -> Unit) {
    val pages = if (state.authorIndex) {
        listOf(LibraryMenuPage.Browse, LibraryMenuPage.Folders)
    } else {
        listOf(
            LibraryMenuPage.Browse,
            LibraryMenuPage.Folders,
            LibraryMenuPage.Layout,
            LibraryMenuPage.Sort,
        )
    }
    pages.forEach { page ->
        ContextMenuItem(
            stringResource(R.string.library_submenu, stringResource(page.title)),
            enabled = state.folders.isNotEmpty(),
        ) {
            select(page)
        }
    }
}

@Composable
private fun LibraryMenuChoices(
    page: LibraryMenuPage,
    state: LibraryUiState,
    actions: LibraryActions,
): List<LibraryMenuChoice> = when (page) {
    LibraryMenuPage.Browse -> LibraryBrowse.entries.map { mode ->
        LibraryMenuChoice(
            stringResource(
                if (mode ==
                    LibraryBrowse.Books
                ) {
                    R.string.library_all_books
                } else {
                    R.string.library_authors
                },
            ),
            mode == state.options.browse,
        ) { actions.browse(mode) }
    }

    LibraryMenuPage.Folders -> (listOf<String?>(null) + state.folders.map { it.uri }).map { uri ->
        LibraryMenuChoice(
            uri?.let(::sourceFolderLabel) ?: stringResource(R.string.all_book_folders),
            uri == state.options.folderUri,
        ) { actions.folder(uri) }
    }

    LibraryMenuPage.Layout -> LibraryViewMode.entries.map { view ->
        LibraryMenuChoice(
            stringResource(
                if (view ==
                    LibraryViewMode.Grid
                ) {
                    R.string.library_layout_grid
                } else {
                    R.string.library_layout_list
                },
            ),
            view == state.options.viewMode,
        ) {
            if (view != state.options.viewMode) actions.toggleView()
        }
    }

    LibraryMenuPage.Sort -> LibrarySort.entries.map { sort ->
        LibraryMenuChoice(stringResource(sort.label()), sort == state.options.sort) {
            actions.sort(sort)
        }
    }

    LibraryMenuPage.Root -> emptyList()
}

private fun LibrarySort.label() = when (this) {
    LibrarySort.Recent -> R.string.library_sort_recent
    LibrarySort.Title -> R.string.library_sort_title
    LibrarySort.Progress -> R.string.library_sort_progress
}
