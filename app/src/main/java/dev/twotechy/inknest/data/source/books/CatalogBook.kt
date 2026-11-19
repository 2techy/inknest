package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.SourceDocument

internal data class CatalogBook(
    val path: String,
    val relative: List<String>,
    val document: SourceDocument,
)
