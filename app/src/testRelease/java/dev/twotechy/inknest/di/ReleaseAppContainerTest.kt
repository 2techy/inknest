package dev.twotechy.inknest.di

import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingStatsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAppContainerTest {
    @Test
    fun `release container wires with no fixtures or preview controller`() = runTest {
        val fakePrefs = object : UserPreferencesRepository {
            override val appPreferences = kotlinx.coroutines.flow.flowOf(
                dev.twotechy.inknest.model.AppPreferences(),
            )
            override suspend fun setDisplayMode(mode: dev.twotechy.inknest.model.DisplayMode) = Unit
            override suspend fun setShowHomeStats(show: Boolean) = Unit
            override suspend fun setShowHomePinnedApps(show: Boolean) = Unit
            override suspend fun completeOnboarding() = Unit
            override val pinnedComponentIds = flowOf(emptyList<String>())
            override suspend fun pin(componentId: String) = Unit
            override suspend fun unpin(componentId: String) = Unit
            override suspend fun movePin(componentId: String, offset: Int) = Unit
            override suspend fun pruneStalePins(validComponentIds: Set<String>) = Unit
        }

        val container = AppContainer(
            readingRepository = UnconnectedReadingRepository(),
            installedAppsRepository = UnconnectedInstalledAppsRepository(),
            readingStatsRepository = UnconnectedReadingStatsRepository(),
            userPreferencesRepository = fakePrefs,
            appLauncher = AppLauncher(android.content.ContextWrapper(null)),
            appIconLoader = AppIconLoader(android.content.ContextWrapper(null)),
            launcherModeController = dev.twotechy.inknest.platform.LauncherModeController(
                android.content.ContextWrapper(null),
            ),
            homePreviewController = null,
            acknowledgementsRepository = AcknowledgementsRepository { "notices" },
        )

        assertEquals(ReadingState.Empty, container.readingRepository.reading.first())
        assertTrue(container.installedAppsRepository.pinnedApps.first().isEmpty())
        assertNull(container.readingStatsRepository.stats.first())
        assertNull(container.homePreviewController)
    }
}
