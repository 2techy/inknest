package dev.twotechy.inknest.data

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.cancellableQuery
import dev.twotechy.inknest.data.source.epub.EpubProbe
import dev.twotechy.inknest.data.source.safProviderOperation
import dev.twotechy.inknest.data.source.stats.InvalidStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingProbeTest {
    private fun workspace(): File = Files.createTempDirectory(
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.toPath(),
        "probe-test-",
    ).toFile()

    private fun archive(file: File, cover: String = "cover.jpg", dtd: Boolean = false) {
        val container = """<?xml version="1.0"?>
            ${if (dtd) "<!DOCTYPE container [<!ENTITY external SYSTEM 'file:///not-readable'>]>" else ""}
            <container><rootfiles><rootfile full-path="OPS/book.opf"/></rootfiles></container>"""
        val opf = """<package xmlns:dc="http://purl.org/dc/elements/1.1/">
            <metadata><dc:title>Tiếng Việt</dc:title><dc:creator>Author</dc:creator>
            <meta name="cover" content="cover"/></metadata>
            <manifest><item id="cover" href="$cover" properties="cover-image"/></manifest></package>"""
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val image = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, image)
        bitmap.recycle()
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in mapOf(
                "META-INF/container.xml" to container.toByteArray(),
                "OPS/book.opf" to opf.toByteArray(),
                "OPS/cover.jpg" to image.toByteArray(),
            )) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    @Test fun epubMetadataAndCoverAreBoundedToTheArchive() = runBlocking {
        val dir = workspace()
        try {
            val file = File(dir, "book.epub")
            archive(file)
            val result = EpubProbe().read(file)
            assertEquals("Tiếng Việt", result.title)
            assertEquals("Author", result.author)
            assertTrue(result.cover)
            for ((cover, dtd) in listOf("../../outside.jpg" to false, "cover.jpg" to true)) {
                archive(file, cover, dtd)
                try {
                    EpubProbe().read(file)
                    fail("Unsafe archive should fail")
                } catch (
                    _: Exception,
                ) { }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private class Documents(private val root: File) : SourceDocumentReader {
        var dbMetadataReads = 0
        var changeOnce = false
        var providerFailures = 0
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            val file = File(root, path.last())
            checkWalMetadata(path)
            if (!file.exists()) throw MissingSourceFile()
            val modified = if (path.last() == "statistics.sqlite3") {
                dbMetadataReads++
                file.lastModified() + if (changeOnce && dbMetadataReads == 2) 1 else 0
            } else {
                file.lastModified()
            }
            return SourceDocument(
                file.toURI().toString(),
                "application/octet-stream",
                file.length(),
                modified,
            )
        }
        private fun checkWalMetadata(path: List<String>) {
            if (path.last().endsWith("-wal") && providerFailures > 0) {
                providerFailures--
                safProviderOperation { throw IllegalArgumentException("provider document missing") }
            }
        }

        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            val file = File(root, path.last())
            if (!file.exists()) throw MissingSourceFile()
            return file.inputStream().use { parse(it) }
        }
    }

    @Test fun snapshotReadsWalAndRetriesFreshDirectories() = runBlocking {
        val dir = workspace()
        val source = File(dir, "source").apply { mkdir() }
        val cache = File(dir, "snapshots").apply { mkdir() }
        val db = statisticsDatabase(source)
        try {
            val documents = Documents(source).apply { changeOnce = true }
            val result = StatisticsSnapshotProbe(documents, cache).read("tree")
            assertEquals(1L, result.books)
            assertEquals(1L, result.pageRows)
            assertTrue(result.walPresent)
            assertTrue(documents.dbMetadataReads >= 4)
            assertTrue(cache.listFiles().orEmpty().isEmpty())
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 120, 1)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, NULL, 2)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, -60, 3)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 'invalid', 4)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 0.5, 5)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1e30, 60, 6)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 60, NULL)")
            db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 60, 'invalid')")
            val events = StatisticsSnapshotProbe(documents, cache).events("tree")
            assertEquals(4, events.size)
            assertEquals(300L, events.sumOf { it.seconds })
            assertEquals(2, events.count { it.page == 1L })
            assertEquals(2, events.count { it.page == null })
            assertEquals(1000L, events.first().startedAt)
            assertTrue(cache.listFiles().orEmpty().isEmpty())
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }

    private fun statisticsDatabase(source: File): SQLiteDatabase {
        val db = SQLiteDatabase.openOrCreateDatabase(File(source, "statistics.sqlite3"), null)
        db.enableWriteAheadLogging()
        db.execSQL("CREATE TABLE book(id INTEGER, title TEXT, authors TEXT)")
        db.execSQL(
            "CREATE TABLE page_stat_data(id_book INTEGER, start_time INTEGER, duration INTEGER, page INTEGER)",
        )
        db.execSQL("INSERT INTO book VALUES(1, 'Title', 'Author')")
        db.execSQL("INSERT INTO page_stat_data VALUES(1, 1, 60, 1)")
        return db
    }

    @Test fun disappearingWalProviderErrorRecoversOnThirdAttempt() = runBlocking {
        val dir = workspace()
        val source = File(dir, "source").apply { mkdir() }
        val cache = File(dir, "snapshots").apply { mkdir() }
        val db = statisticsDatabase(source)
        try {
            val documents = Documents(source).apply { providerFailures = 2 }
            val result = StatisticsSnapshotProbe(documents, cache).read("tree")
            assertEquals(1L, result.books)
            assertEquals(1L, result.pageRows)
            assertTrue(result.walPresent)
            assertEquals(0, documents.providerFailures)
            assertEquals(4, documents.dbMetadataReads)
            assertTrue(cache.listFiles().orEmpty().isEmpty())
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }

    @Test fun invalidSnapshotsAreRejectedAndCleanedUp() = runBlocking {
        for (corrupt in listOf(false, true)) {
            val dir = workspace()
            val source = File(dir, "source").apply { mkdir() }
            val cache = File(dir, "snapshots").apply { mkdir() }
            try {
                val file = File(source, "statistics.sqlite3")
                val expected = if (corrupt) {
                    file.writeText("not a database")
                    InvalidStatisticsSnapshot::class.java
                } else {
                    SQLiteDatabase.openOrCreateDatabase(file, null).use {
                        it.execSQL("CREATE TABLE unrelated(id INTEGER)")
                    }
                    UnsupportedStatisticsSchema::class.java
                }
                try {
                    StatisticsSnapshotProbe(Documents(source), cache).read("tree")
                    fail("Invalid snapshot accepted")
                } catch (error: java.io.IOException) {
                    assertTrue(
                        "Expected ${expected.simpleName}, got $error",
                        expected.isInstance(error),
                    )
                }
                assertTrue(cache.listFiles().orEmpty().isEmpty())
            } finally {
                dir.deleteRecursively()
            }
        }
    }

    @Test fun sqliteCancellationInterruptsQueryAndAllowsNextQuery() = runBlocking {
        SQLiteDatabase.create(null).use { db ->
            try {
                withTimeout(100) {
                    withContext(Dispatchers.IO) {
                        cancellableQuery { signal ->
                            db.rawQuery(
                                "WITH RECURSIVE c(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM c WHERE x<1000000000) SELECT SUM(x) FROM c",
                                null,
                                signal,
                            )
                                .use {
                                    it.moveToFirst()
                                    it.getLong(0)
                                }
                        }
                    }
                }
                fail("Expected timeout")
            } catch (_: TimeoutCancellationException) { }
            db.rawQuery("SELECT 1", null).use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        }
    }
}
