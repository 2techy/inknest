package dev.twotechy.inknest.model

data class ReadingSourceConfig(
    val sourceId: String? = null,
    val koreaderTreeUri: String? = null,
    val booksTreeUri: String? = null,
    val sourcePackage: String? = null,
    val openMode: ReadingOpenMode = ReadingOpenMode.Unverified,
    val verifiedAppVersion: String? = null,
)

enum class SourceAccessState {
    NotSelected,
    Granted,
    PermissionLost,
    InvalidFolder,
    Unsupported,
    ProviderError,
}

enum class ReadingOpenMode { Unverified, SourceAppOnly, DirectConfirmed }
