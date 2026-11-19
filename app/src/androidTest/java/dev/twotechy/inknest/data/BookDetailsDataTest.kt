package dev.twotechy.inknest.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.local.database.library.LibraryBookEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsBookEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsEventEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsImportEntity
import dev.twotechy.inknest.data.source.formats.epub.epubDescription
import dev.twotechy.inknest.data.source.formats.xml.readBoundedXml
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BookDetailsDataTest {
    @Test fun descriptionCleansHtmlAndPreservesNestedParagraphsWithinLimit() {
        fun parse(value: String): String? =
            epubDescription(readBoundedXml(value.toByteArray()).descendants("description").single())
        assertEquals(
            "First & second\n\nThird",
            parse("<description><![CDATA[<p>First &amp; second</p><p>Third</p>]]></description>"),
        )
        val nested = parse("<description><p>First <b>bold</b> last</p><p>Next</p></description>")!!
        assertTrue(nested.contains("First bold last"))
        assertTrue(nested.contains("\n"))
        assertTrue(nested.endsWith("Next"))
        assertNull(parse("<description>  </description>"))
        val large = "<description>${"x".repeat(40_000)}</description>"
        assertEquals(32 * 1024, parse(large)!!.length)
    }

    @Test fun dayPagesStayBoundedAndTotalsDoNotDependOnOffsets() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(context.cacheDir.toPath(), "book-pages-").toFile()
        val database = Room.databaseBuilder<ReadingDatabase>(
            context,
            directory.resolve("reading.db").path,
        )
            .setDriver(AndroidSQLiteDriver()).build()
        try {
            val dao = database.statisticsDao()
            for (count in listOf(0, 1, 5, 6, 12)) {
                val generation = "g$count"
                dao.putBooks(listOf(StatisticsBookEntity(generation, 1, "hash")))
                dao.putEvents(
                    (0 until count).map { day ->
                        StatisticsEventEntity(
                            generation,
                            day,
                            day.toLong() + 1,
                            60,
                            1,
                            1,
                            100,
                            day.toLong(),
                        )
                    },
                )
                val all = mutableListOf<Long>()
                for (offset in 0 until count step 5) {
                    val rows = dao.observeBookDays(generation, "hash", 100, 6, offset).first()
                    assertEquals(minOf(6, count - offset), rows.size)
                    all += rows.take(5).map { it.day }
                }
                assertEquals((0 until count).reversed().map(Int::toLong), all)
                val totals = dao.observeBookTotals(generation, "hash", 100).first()
                assertEquals(count, totals.days)
                assertEquals(count * 60L, totals.seconds)
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    @Test fun bookQueriesDeduplicateAndIsolateSnapshots() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "book-details-test-",
        ).toFile()
        val database = Room.databaseBuilder<ReadingDatabase>(
            context,
            directory.resolve("reading.db").path,
        ).setDriver(AndroidSQLiteDriver()).build()
        try {
            val dao = database.statisticsDao()
            dao.putBooks(
                listOf(
                    StatisticsBookEntity("g", 1, "hash"),
                    StatisticsBookEntity("g", 2, "hash"),
                    StatisticsBookEntity("g", 3, "different"),
                ),
            )
            val rows = listOf(
                StatisticsEventEntity("g", 0, 1, 60, 1, 1, 100, 10),
                StatisticsEventEntity("g", 1, 2, 60, 1, 1, 100, 10),
                StatisticsEventEntity("g", 2, 3, 60, 1, 1, 200, 10),
                StatisticsEventEntity("g", 3, 4, 60, 2, 1, 100, 10),
                StatisticsEventEntity("g", 4, 5, 30, 1, null, null, 11),
                StatisticsEventEntity("g", 5, 6, 999, 3, 2, 100, 10),
                StatisticsEventEntity("g", 6, 7, 999, 1, 3, 100, 99),
            )
            dao.putEvents(rows)
            val total = dao.observeBookTotals("g", "hash", 11).first()
            assertEquals(270L, total.seconds)
            assertNull(total.pages)
            assertEquals(2, total.days)
            val days = dao.observeBookDays("g", "hash", 11, 1).first()
            assertEquals(listOf(11L), days.map { it.day })
            assertEquals(3L, dao.observeBookDays("g", "hash", 10, 30).first().single().pages)
            assertEquals(0, dao.observeBookTotals("g", "missing", 11).first().days)
            assertEquals(0, dao.observeMatches("other-generation", "hash").first())
            dao.copyBooks("g", "rebinned")
            assertEquals(2, dao.observeMatches("rebinned", "hash").first())

            val inventory = database.libraryDao()
            inventory.putBooks(
                listOf(LibraryBookEntity("source", "folder", "path", "Book", fingerprint = "1:1")),
            )
            inventory.saveChecksum("source", "folder", "path", "old", "bad")
            assertNull(inventory.checksum("source", "folder", "path", "1:1"))
            inventory.saveChecksum("source", "folder", "path", "1:1", "hash")
            assertEquals("hash", inventory.checksum("source", "folder", "path", "1:1"))
            val operation = StatisticsImportEntity("pending", "tree")
            dao.begin(operation)
            database.readingDao().clearAll()
            inventory.saveChecksum("source", "folder", "path", "1:1", "hash")
            assertNull(inventory.book("source", "folder", "path"))
            assertEquals(0, dao.observeMatches("g", "hash").first())
            try {
                dao.appendBooks(operation, listOf(StatisticsBookEntity("g", 1, "hash")))
                fail("Cleared imports must not restore identities")
            } catch (_: CancellationException) {
                assertEquals(0, dao.observeMatches("g", "hash").first())
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
