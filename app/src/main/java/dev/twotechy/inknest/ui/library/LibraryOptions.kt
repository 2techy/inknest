package dev.twotechy.inknest.ui.library

internal data class LibraryOptions(
    val query: String = "",
    val searching: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.All,
    val sort: LibrarySort = LibrarySort.Recent,
    val viewMode: LibraryViewMode = LibraryViewMode.Grid,
    val folderUri: String? = null,
    val browse: LibraryBrowse = LibraryBrowse.Books,
    val authorKey: String? = null,
    val authorName: String? = null,
)
