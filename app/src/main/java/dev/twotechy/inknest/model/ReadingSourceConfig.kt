package dev.twotechy.inknest.model

data class ReadingSourceConfig(
    val sourceId: String? = null,
    val koreaderTreeUri: String? = null,
    val booksTreeUris: Set<String> = emptySet(),
    val sourcePackage: String? = null,
) {
    val grantedTrees: Set<String> get() = booksTreeUris + listOfNotNull(koreaderTreeUri)
}

enum class SourceAccessState {
    NotSelected,
    Granted,
    PermissionLost,
    InvalidFolder,
    Unsupported,
    ProviderError,
}
