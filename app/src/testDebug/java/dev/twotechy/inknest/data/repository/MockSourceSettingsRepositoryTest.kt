package dev.twotechy.inknest.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSourceSettingsRepositoryTest {
    @Test fun `failed source change retains active folders and cache`() = runTest {
        val repo = MockSourceSettingsRepository()
        repo.connect(SourceFolder.KOReader)
        repo.connect(SourceFolder.Books)
        repo.refresh()
        val previous = repo.state.value
        repo.failNextAction()
        val failed = runCatching { repo.connect(SourceFolder.KOReader) }
        assertTrue(failed.isFailure)
        assertEquals(previous, repo.state.value)
        repo.failNextAction()
        assertTrue(runCatching { repo.refresh() }.isFailure)
        assertEquals(previous, repo.state.value)
    }

    @Test fun `clear cache preserves folders and disconnect resets only source state`() = runTest {
        val repo = MockSourceSettingsRepository()
        repo.connect(SourceFolder.KOReader)
        repo.connect(SourceFolder.Books)
        repo.refresh()
        repo.clearCache()
        assertNotNull(repo.state.value.koreaderFolder)
        assertNotNull(repo.state.value.booksFolder)
        assertEquals(0L, repo.state.value.cacheBytes)
        assertNull(repo.state.value.lastUpdated)
        repo.disconnect()
        assertEquals(SourceStatus.NotConnected, repo.state.value.status)
        assertNull(repo.state.value.koreaderFolder)
        assertTrue(repo.state.value.isSample)
    }
}
