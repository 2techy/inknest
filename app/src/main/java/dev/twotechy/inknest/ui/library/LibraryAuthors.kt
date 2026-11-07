package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.model.ReadingItem
import java.util.Locale

private val authorWhitespace = Regex("[\\s\\p{Z}]+")

private fun authorName(value: String?): String? =
    value?.trim()?.replace(authorWhitespace, " ")?.takeIf {
        it.isNotEmpty()
    }
internal fun libraryAuthorKey(value: String?): String =
    authorName(value)?.let { "known:" + it.lowercase(Locale.ROOT) } ?: "unknown:"
internal fun libraryAuthors(books: List<ReadingItem>): List<LibraryAuthor> =
    books.groupBy { libraryAuthorKey(it.author) }.map { (key, items) ->
        LibraryAuthor(key, authorName(items.first().author), items.size)
    }.sortedWith(compareBy<LibraryAuthor> { it.name == null }.thenBy { it.key })
