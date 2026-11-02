package dev.twotechy.inknest.di

import dev.twotechy.inknest.data.repository.ReadingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAppContainerTest {
    @Test fun `release starts disconnected with no fixtures or preview controller`() = runTest {
        val container = createAppContainer()
        assertEquals(ReadingState.Empty, container.readingRepository.reading.first())
        assertTrue(container.installedAppsRepository.pinnedApps.first().isEmpty())
        assertNull(container.readingStatsRepository.stats.first())
        assertNull(container.homePreviewController)
    }
}
