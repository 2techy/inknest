package dev.twotechy.inknest.model

internal const val KOREADER_PACKAGE = "org.koreader.launcher"

data class ReadingSourceConfig(
    val sourceId: String? = null,
    val koreaderTreeUri: String? = null,
    val booksTreeUris: Set<String> = emptySet(),
    val sourcePackage: String? = null,
) {
    val grantedTrees: Set<String> get() = booksTreeUris + listOfNotNull(koreaderTreeUri)
}
