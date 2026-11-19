package dev.twotechy.inknest.data.repository.sources

import kotlinx.coroutines.flow.StateFlow

interface SourceSettingsRepository {
    val state: StateFlow<SourceSettingsState>
    suspend fun selectApp(packageName: String): Unit = error("Source app selection unavailable")
    suspend fun connectTree(selection: SourceTreeSelection): Unit =
        error("Tree selection unavailable")
    suspend fun removeBookTree(uri: String): Unit = error("Book folder removal unavailable")
    suspend fun disconnect()
    suspend fun refresh()
    suspend fun clearCache()
}
