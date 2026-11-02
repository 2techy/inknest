package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.PinnedApp
import dev.twotechy.inknest.model.ReadingStats
import kotlinx.coroutines.flow.flowOf

// Production defaults until M2/M4/M8/M11 connect real sources. Never emit sample data.
internal class UnconnectedReadingRepository : ReadingRepository {
    override val reading = flowOf(ReadingState.Empty)
    override suspend fun refresh() = Unit
}

internal class UnconnectedInstalledAppsRepository : InstalledAppsRepository {
    override val pinnedApps = flowOf(emptyList<PinnedApp>())
}

internal class UnconnectedReadingStatsRepository : ReadingStatsRepository {
    override val stats = flowOf<ReadingStats?>(null)
}
