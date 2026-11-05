package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingStatus
import java.util.Locale

// The repository's list order is the source history order; never infer identity from a title.
internal fun selectLibraryBooks(
    books: List<ReadingItem>,
    options: LibraryOptions,
): List<ReadingItem> {
    val query = options.query.trim()
    return books.withIndex().filter { (_, book) ->
        book.matches(options.filter) && book.matches(query)
    }.sortedWith(
        bookOrder(options.sort).thenBy { it.index }
            .thenBy { it.value.id.sourceId }.thenBy { it.value.id.path },
    )
        .map { it.value }
}

private fun ReadingItem.matches(query: String): Boolean = query.isEmpty() ||
    title.contains(query, ignoreCase = true) || author?.contains(query, ignoreCase = true) == true

private fun ReadingItem.matches(filter: LibraryFilter): Boolean = when (filter) {
    LibraryFilter.All -> true
    LibraryFilter.Reading -> status == ReadingStatus.Reading
    LibraryFilter.Finished -> status == ReadingStatus.Finished
    LibraryFilter.Unknown -> status == ReadingStatus.Unknown
}

private fun bookOrder(sort: LibrarySort): Comparator<IndexedValue<ReadingItem>> = when (sort) {
    LibrarySort.Recent -> compareByDescending<IndexedValue<ReadingItem>> {
        it.value.lastReadAt !=
            null
    }
        .thenByDescending { it.value.lastReadAt }

    LibrarySort.Title -> compareBy<IndexedValue<ReadingItem>> { it.value.title.isBlank() }
        .thenBy { it.value.title.lowercase(Locale.ROOT) }

    LibrarySort.Progress -> compareByDescending<IndexedValue<ReadingItem>> {
        it.value.progress !=
            null
    }
        .thenByDescending { it.value.progress }
}
