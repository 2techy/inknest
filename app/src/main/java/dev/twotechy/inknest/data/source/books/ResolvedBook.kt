package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.SourceDocument
import java.io.IOException

internal data class ResolvedBook(
    val sourcePath: String,
    val tree: String,
    val relative: List<String>,
    val document: SourceDocument,
)
internal class UnresolvedBookPath : IOException()
