package dev.twotechy.inknest.data.source.books

internal data class BookMetadata(
    val fingerprint: String?,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val description: String? = null,
)
