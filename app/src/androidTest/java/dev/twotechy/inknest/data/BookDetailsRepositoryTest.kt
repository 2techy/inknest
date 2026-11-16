package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.LibraryBookEntity
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.StatisticsBookEntity
import dev.twotechy.inknest.data.repository.CachedBookDetailsRepository
import dev.twotechy.inknest.data.repository.LibraryCatalog
import dev.twotechy.inknest.data.repository.LibraryRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.StatisticsRepository
import dev.twotechy.inknest.data.source.BookChecksum
import dev.twotechy.inknest.data.source.BookChecksumSource
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.platform.clock.StatisticsMoment
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailsRepositoryTest {
    @Test fun checksumMatchingEmptyHistoryCacheAndSourceIsolation() = runBlocking {
        withTimeout(20_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val directory = Files.createTempDirectory(
                context.cacheDir.toPath(),
                "details-repo-",
            ).toFile()
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
            ) {
                File(directory, "source.preferences_pb")
            }
            val database = Room.databaseBuilder<ReadingDatabase>(
                context,
                directory.resolve("reading.db").path,
            )
                .setDriver(AndroidSQLiteDriver()).build()
            try {
                val config = ReadingSourceConfigRepository(store)
                val before = config.config.first()
                val selected = config.treeCandidate(before, SourceFolder.KOReader, "reader")
                    .copy(booksTreeUris = setOf("books"))
                config.activateTree(before, selected)
                val id = ReadingItemId(checkNotNull(selected.sourceId), "primary:Books/sample.epub")
                val book = ReadingItem(id, "Book", "Author", null, description = "Description")
                val library = object : LibraryRepository {
                    override val catalog =
                        MutableStateFlow(LibraryCatalog(books = listOf(book), configured = true))
                    override suspend fun refresh() {}
                }
                val statistics = statisticsFixture()
                val clock = object : StatisticsClock {
                    override val moments =
                        MutableStateFlow(
                            StatisticsMoment(LocalDate.of(2026, 11, 16), ZoneId.of("UTC")),
                        )
                }
                seed(database, id)
                val source = TestBookChecksumSource()
                val repository =
                    CachedBookDetailsRepository(
                        library,
                        config,
                        database.libraryDao(),
                        database.statisticsDao(),
                        statistics,
                        source,
                        clock,
                    )
                verifyBooks(repository, id, config, source)
            } finally {
                database.close()
                job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
    }

    private fun statisticsFixture(): StatisticsRepository = object : StatisticsRepository {
        override val data = MutableStateFlow(
            StatisticsData(
                generation = "g",
                zone = ZoneId.of("UTC"),
                configured = true,
                updatedAt = 1L,
                sourceTree = "reader",
            ),
        )
        override val stats = flowOf<dev.twotechy.inknest.model.ReadingStats?>(null)
        override suspend fun refresh() {}
    }

    private suspend fun seed(database: ReadingDatabase, id: ReadingItemId) {
        database.libraryDao().putBooks(
            listOf(
                LibraryBookEntity(
                    id.sourceId,
                    "books",
                    id.path,
                    "Book",
                    fingerprint = "1:1",
                ),
            ),
        )
        database.statisticsDao().putBooks(listOf(StatisticsBookEntity("g", 1, "matching")))
    }

    private suspend fun verifyBooks(
        repository: CachedBookDetailsRepository,
        id: ReadingItemId,
        config: ReadingSourceConfigRepository,
        source: TestBookChecksumSource,
    ) {
        val empty = repository.observe(id, 30).first {
            it.statistics.status ==
                BookStatisticsStatus.Ready
        }
        assertEquals(0, empty.statistics.readingDays)
        assertEquals(0L, empty.statistics.seconds)
        assertEquals("Description", empty.book?.description)
        repository.observe(id, 30).first {
            it.statistics.status ==
                BookStatisticsStatus.Ready
        }
        assertTrue(source.cacheHit)
        source.checksum = "different"
        repository.retry()
        repository.observe(id, 30).first {
            it.statistics.status ==
                BookStatisticsStatus.Unlinked
        }
        source.denied = true
        val inaccessible = repository.observe(id, 30).first {
            it.book?.availability ==
                BookAvailability.PermissionLost
        }
        assertEquals(BookStatisticsStatus.Unlinked, inaccessible.statistics.status)
        config.disconnect()
        val disconnected = repository.observe(id, 30).first {
            it.book?.availability ==
                BookAvailability.Unresolved
        }
        assertTrue(disconnected.statistics.status != BookStatisticsStatus.Ready)
    }
}

private class TestBookChecksumSource : BookChecksumSource {
    var denied = false
    var checksum = "matching"
    var cacheHit = false
    override suspend fun checksum(
        config: dev.twotechy.inknest.model.ReadingSourceConfig,
        path: String,
        cached: suspend (String, String) -> String?,
    ): BookChecksum {
        if (denied) throw SecurityException("No grant")
        cacheHit = cached("books", "1:1") == checksum
        return BookChecksum("books", "1:1", checksum)
    }
}
