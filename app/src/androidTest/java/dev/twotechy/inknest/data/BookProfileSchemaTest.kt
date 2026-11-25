package dev.twotechy.inknest.data

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.local.database.library.LibraryBookRow
import dev.twotechy.inknest.data.local.database.reading.ReadingBookRow
import dev.twotechy.inknest.data.local.database.reading.ReadingSourceEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsBookRow
import dev.twotechy.inknest.data.local.database.statistics.StatisticsSourceEntity
import dev.twotechy.inknest.data.source.statistics.impl.readStatisticsBooks
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookProfileSchemaTest {
    @Test fun version8CacheIsDiscarded() = withBookDatabase(::seedVersion8) { database ->
        assertNull(database.readingDao().book("source", "snapshot", BOOK_PATH))
        assertNull(database.libraryDao().book("source", "folder", BOOK_PATH))
        assertNull(database.statisticsDao().observeSnapshot("tree").first())
    }

    @Test fun fileVersionsShareProfilesAndStatisticsKeepsSourceOnlyBooks() = withBookDatabase {
        seedProfiles(it)
        verifyFileProfiles(it)
        verifyStatisticsProfiles(it)
    }
}

private fun withBookDatabase(
    seed: (String) -> Unit = {},
    verify: suspend (ReadingDatabase) -> Unit,
) = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val dir = Files.createTempDirectory(context.cacheDir.toPath(), "book-profile-test-").toFile()
    val path = dir.resolve("reading.db").path
    try {
        seed(path)
        val database = ReadingDatabase.create(context, path)
        try {
            verify(database)
        } finally {
            database.close()
        }
    } finally {
        dir.deleteRecursively()
    }
}

private suspend fun seedProfiles(database: ReadingDatabase) {
    database.readingDao().save(
        ReadingSourceEntity("source", "snapshot", currentPath = BOOK_PATH),
        listOf(
            ReadingBookRow(
                "source",
                "snapshot",
                BOOK_PATH,
                "History",
                "Available",
                progress = 42,
                historyOrder = 0,
                fingerprint = "revision1",
            ),
        ),
        replaceHistory = true,
    )
    database.libraryDao().putBooks(
        listOf(
            LibraryBookRow(
                "source",
                "folder",
                BOOK_PATH,
                "Catalog",
                author = "Author",
                fingerprint = "revision1",
                checksum = "hash",
            ),
        ),
    )
}

private fun seedVersion8(path: String) {
    val assets = InstrumentationRegistry.getInstrumentation().context.assets
    val schema = assets.open(
        "dev.twotechy.inknest.data.local.database.ReadingDatabase/8.json",
    ).use {
        JSONObject(it.bufferedReader().readText()).getJSONObject("database")
    }
    AndroidSQLiteDriver().open(path).use { connection ->
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            connection.execSQL(entity.getString("createSql").replace(TABLE_PLACEHOLDER, table))
            val indices = entity.optJSONArray("indices")
            repeat(indices?.length() ?: 0) { j ->
                connection.execSQL(
                    checkNotNull(indices).getJSONObject(j).getString("createSql")
                        .replace(TABLE_PLACEHOLDER, table),
                )
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        repeat(setup.length()) { i -> connection.execSQL(setup.getString(i)) }
        connection.execSQL("PRAGMA user_version=8")
        connection.execSQL(
            """
            INSERT INTO reading_items (sourceId, snapshotKey, path, title, availability, progress, fingerprint, historyOrder)
            VALUES ('source','snapshot','primary:Book.epub','History','Available',42,'revision1',0)
            """,
        )
        connection.execSQL(
            """
            INSERT INTO library_books (sourceId, folderKey, path, title, author, fingerprint, status, checksum)
            VALUES ('source','folder','primary:Book.epub','Catalog','Author','revision1','Unknown','hash')
            """,
        )
        connection.execSQL("INSERT INTO statistics_sources VALUES ('tree','old',1,'UTC')")
        connection.execSQL("INSERT INTO statistics_books VALUES ('old',1,'hash')")
        connection.execSQL("INSERT INTO statistics_days VALUES ('old',100,60,2)")
    }
}

private const val TABLE_PLACEHOLDER = "\${TABLE_NAME}"
private const val BOOK_PATH = "primary:Book.epub"

private suspend fun verifyFileProfiles(database: ReadingDatabase) {
    val reading = database.readingDao()
    val library = database.libraryDao()
    val history = checkNotNull(reading.book("source", "snapshot", BOOK_PATH))
    assertEquals("Catalog", history.title)
    assertEquals("Author", history.author)
    assertEquals(42, history.progress)
    assertEquals(0, history.historyOrder)

    val original = checkNotNull(library.book("source", "folder", BOOK_PATH))
    library.putBooks(listOf(original.copy(title = "Updated metadata", checksum = null)))
    assertEquals("Updated metadata", reading.book("source", "snapshot", BOOK_PATH)?.title)
    assertEquals("hash", library.checksum("source", "folder", BOOK_PATH, "revision1"))
    val replacement = original.copy(
        title = "Replacement",
        fingerprint = "revision2",
        checksum = null,
    )
    library.putBooks(listOf(replacement))
    assertEquals("Replacement", library.book("source", "folder", BOOK_PATH)?.title)
    assertEquals("Updated metadata", reading.book("source", "snapshot", BOOK_PATH)?.title)
    library.removeFolder("source", "folder")
    assertNull(library.book("source", "folder", BOOK_PATH))
    assertEquals("Updated metadata", reading.book("source", "snapshot", BOOK_PATH)?.title)
}

private suspend fun verifyStatisticsProfiles(database: ReadingDatabase) {
    val statistics = database.statisticsDao()
    val library = database.libraryDao()

    statistics.putBooks(listOf(StatisticsBookRow("new", 1, "hash", "KOReader title")))
    SQLiteDatabase.create(null).use { source ->
        source.execSQL("CREATE TABLE book(id INTEGER, title TEXT)")
        source.execSQL("INSERT INTO book VALUES(2, 'Stats only')")
        readStatisticsBooks(source) { books ->
            statistics.putBooks(
                books.map {
                    StatisticsBookRow("new", it.bookId, it.checksum, it.title)
                },
            )
        }
    }
    assertNull(statistics.observeBookProfile("new", 2, "source").first())
    statistics.putSource(StatisticsSourceEntity("tree", "new", 2, "UTC"))
    val linked = checkNotNull(statistics.observeBookProfile("new", 1, "source").first())
    val standalone = checkNotNull(statistics.observeBookProfile("new", 1, "other").first())
    assertNotEquals(standalone.id, linked.id)
    assertEquals("Updated metadata", linked.title)
    assertEquals("KOReader title", standalone.title)
    assertEquals("Stats only", statistics.observeBookProfile("new", 2, "source").first()?.title)

    library.putBooks(
        listOf(
            LibraryBookRow(
                "source",
                "other",
                "primary:Copy.epub",
                "Copy",
                fingerprint = "r",
                checksum = "hash",
            ),
        ),
    )
    assertEquals(standalone.id, statistics.observeBookProfile("new", 1, "source").first()?.id)
    statistics.copyBooks("new", "rebinned")
    statistics.putSource(StatisticsSourceEntity("tree", "rebinned", 2, "UTC"))
    statistics.remove("new")
    assertEquals(
        "Stats only",
        statistics.observeBookProfile("rebinned", 2, "source").first()?.title,
    )
}
