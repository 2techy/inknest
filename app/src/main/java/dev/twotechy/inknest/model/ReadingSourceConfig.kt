package dev.twotechy.inknest.model

data class ReadingSourceConfig(
    val sourceId: String? = null,
    val koreaderTreeUri: String? = null,
    val booksTreeUri: String? = null,
    val sourcePackage: String? = null,
)

enum class SourceAccessState {
    NotSelected,
    Granted,
    PermissionLost,
    InvalidFolder,
    Unsupported,
    ProviderError,
}
