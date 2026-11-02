package dev.twotechy.inknest.di

import dev.twotechy.inknest.data.repository.FakeInstalledAppsRepository
import dev.twotechy.inknest.data.repository.FakeReadingRepository
import dev.twotechy.inknest.data.repository.FakeReadingStatsRepository

internal fun createAppContainer(): AppContainer {
    val reading = FakeReadingRepository()
    return AppContainer(
        reading,
        FakeInstalledAppsRepository(),
        FakeReadingStatsRepository(),
        reading,
    )
}
