package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.model.ReadingItem

internal data class LibraryActions(
    val query: (String) -> Unit,
    val searching: (Boolean) -> Unit,
    val filter: (LibraryFilter) -> Unit,
    val sort: (LibrarySort) -> Unit,
    val toggleView: () -> Unit,
    val clearFilters: () -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
    val open: (ReadingItem) -> Unit,
    val details: (ReadingItem) -> Unit = {},
    val folder: (String?) -> Unit = {},
    val browse: (LibraryBrowse) -> Unit = {},
    val author: (LibraryAuthor) -> Unit = {},
    val backToAuthors: () -> Unit = {},
)
