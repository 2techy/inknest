package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.SourceDocument

internal data class ResolvedBook(
    val sourcePath: String,
    val tree: String,
    val relative: List<String>,
    val document: SourceDocument,
)
