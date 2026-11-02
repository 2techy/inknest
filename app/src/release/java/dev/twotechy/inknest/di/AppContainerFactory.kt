package dev.twotechy.inknest.di

import dev.twotechy.inknest.data.repository.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingStatsRepository

internal fun createAppContainer() = AppContainer(
    readingRepository = UnconnectedReadingRepository(),
    installedAppsRepository = UnconnectedInstalledAppsRepository(),
    readingStatsRepository = UnconnectedReadingStatsRepository(),
)
