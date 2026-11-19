package dev.twotechy.inknest.data.repository.sources

import dev.twotechy.inknest.model.SourceAccessState

data class SourceSettingsState(
    val available: Boolean = false,
    val status: SourceStatus = SourceStatus.NotConnected,
    val koreaderFolder: String? = null,
    val booksFolders: List<BookFolderState> = emptyList(),
    val refreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val cacheBytes: Long? = null,
    val koreaderAccess: SourceAccessState? = null,
    val sourcePackage: String? = null,
)
