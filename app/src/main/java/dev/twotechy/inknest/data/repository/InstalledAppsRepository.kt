package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface InstalledAppsRepository {
    val installedApps: Flow<List<AppInfo>>
    val pinnedApps: Flow<List<PinnedApp>>
    val isLoading: Flow<Boolean>
    val error: Flow<String?> get() = flowOf(null)

    suspend fun reload(retainPackages: Set<String> = emptySet())
    suspend fun pin(componentId: String)
    suspend fun unpin(componentId: String)
    suspend fun movePin(componentId: String, offset: Int)
}
