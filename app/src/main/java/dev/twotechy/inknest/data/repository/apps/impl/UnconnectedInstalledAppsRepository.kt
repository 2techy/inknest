package dev.twotechy.inknest.data.repository.apps.impl

import dev.twotechy.inknest.data.repository.apps.InstalledAppsRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.flow.flowOf

internal class UnconnectedInstalledAppsRepository : InstalledAppsRepository {
    override val installedApps = flowOf(emptyList<AppInfo>())
    override val pinnedApps = flowOf(emptyList<PinnedApp>())
    override val isLoading = flowOf(false)

    override suspend fun reload(retainPackages: Set<String>) = Unit
    override suspend fun pin(componentId: String) = Unit
    override suspend fun unpin(componentId: String) = Unit
    override suspend fun movePin(componentId: String, offset: Int) = Unit
}
