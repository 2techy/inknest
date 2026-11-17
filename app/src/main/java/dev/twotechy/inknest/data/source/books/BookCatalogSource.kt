package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.SourceDocument
internal data class CatalogBook(
    val path: String,
    val relative: List<String>,
    val document: SourceDocument,
)
internal fun interface BookCatalogSource {
    suspend fun scan(tree: String): List<CatalogBook>
}
