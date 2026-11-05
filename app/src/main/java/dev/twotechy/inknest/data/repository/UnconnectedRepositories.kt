package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.flow.flowOf

// Production defaults until M2/M4/M8/M11 connect real sources. Never emit sample data.
internal class UnconnectedReadingRepository : ReadingRepository {
    override val reading = flowOf(ReadingState.Empty)
    override suspend fun refresh() = Unit
}

internal class UnconnectedInstalledAppsRepository : InstalledAppsRepository {
    override val installedApps = flowOf(emptyList<AppInfo>())
    override val pinnedApps = flowOf(emptyList<PinnedApp>())
    override val isLoading = flowOf(false)

    override suspend fun reload(retainPackages: Set<String>) = Unit
    override suspend fun pin(componentId: String) = Unit
    override suspend fun unpin(componentId: String) = Unit
    override suspend fun movePin(componentId: String, offset: Int) = Unit
}
