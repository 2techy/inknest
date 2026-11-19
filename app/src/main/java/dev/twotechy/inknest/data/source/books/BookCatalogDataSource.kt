package dev.twotechy.inknest.data.source.books

internal fun interface BookCatalogDataSource {
    suspend fun scanBooks(tree: String): List<CatalogBook>
}
