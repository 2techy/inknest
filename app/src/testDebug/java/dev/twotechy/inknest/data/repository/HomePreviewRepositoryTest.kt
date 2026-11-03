package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.preview.HomePreviewMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePreviewRepositoryTest {
    @Test fun `preview defaults to Off with no sample reading or statistics`() = runTest {
        val repository = FakeHomePreviewRepository()
        assertEquals(ReadingState.Empty, repository.reading.first())
        assertNull(repository.stats.first())
    }

    @Test fun `turning preview Off removes both reading and statistics`() = runTest {
        val repository = FakeHomePreviewRepository()
        repository.select(HomePreviewMode.Content)
        assertEquals(ReadingState.Content(sampleBook), repository.reading.first())
        assertEquals(sampleStats, repository.stats.first())
        repository.select(HomePreviewMode.Off)
        assertEquals(ReadingState.Empty, repository.reading.first())
        assertNull(repository.stats.first())
    }
}
