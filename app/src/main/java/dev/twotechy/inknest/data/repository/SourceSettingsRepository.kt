package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.SourceAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SourceStatus { NotConnected, Connected, PermissionLost, Error }
enum class SourceFolder { KOReader, Books }

data class SourceSettingsState(
    val available: Boolean = false,
    val status: SourceStatus = SourceStatus.NotConnected,
    val koreaderFolder: String? = null,
    val booksFolder: String? = null,
    val refreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val cacheBytes: Long? = null,
    val koreaderAccess: SourceAccessState? = null,
    val booksAccess: SourceAccessState? = null,
    val sourcePackage: String? = null,
)

interface SourceSettingsRepository {
    val state: StateFlow<SourceSettingsState>
    suspend fun selectApp(packageName: String): Unit = error("Source app selection unavailable")
    suspend fun connectTree(folder: SourceFolder, uri: String, flags: Int): Unit =
        error("Tree selection unavailable")
    suspend fun disconnect()
    suspend fun refresh()
    suspend fun clearCache()
}

internal class UnconnectedSourceSettingsRepository : SourceSettingsRepository {
    override val state = MutableStateFlow(SourceSettingsState()).asStateFlow()
    override suspend fun disconnect(): Unit = error("Reading integration unavailable")
    override suspend fun refresh(): Unit = error("Reading integration unavailable")
    override suspend fun clearCache(): Unit = error("Reading integration unavailable")
}
