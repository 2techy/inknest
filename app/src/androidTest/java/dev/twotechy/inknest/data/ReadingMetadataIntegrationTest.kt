package dev.twotechy.inknest.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.source.BookCoverCache
import dev.twotechy.inknest.data.source.BookMetadataReader
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.KoreaderCurrentSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingMetadataIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = ReadingSourceConfig(
        sourceId = "metadata-test",
        koreaderTreeUri = "content://com.android.externalstorage.documents/tree/primary%3Akoreader",
        booksTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload",
    )

    @Test fun metadataProgressHistoryAndCacheUseExactIdentity() = runBlocking {
        val directory = workspace("metadata-test-")
        val database = Room.inMemoryDatabaseBuilder<ReadingDatabase>(context)
            .setDriver(AndroidSQLiteDriver()).build()
        try {
            val cache = ReadingCache(database.readingDao())
            val covers = BookCoverCache(File(directory, "covers"))
            val documents = Documents(epub())
            val books = BookProbeDataSource(documents, directory)
            val source = KoreaderCurrentSource(
                documents,
                books,
                metadataReader = BookMetadataReader(books, covers, cache::metadata),
            )
            val key = checkNotNull(config.readingCacheKey())
            for (percent in listOf(0.7, 0.3)) {
                documents.percent = percent
                val snapshot = source.read(config)
                cache.save(key, snapshot, 1)
                val item = checkNotNull(snapshot.current)
                assertEquals((percent * 100).toInt(), item.progress)
                assertEquals("Actual title", item.title)
                assertEquals("Author", item.author)
                assertEquals(ReadingStatus.Reading, item.status)
                val file = File(checkNotNull(Uri.parse(item.coverUri).path))
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= 480)
                assertTrue(maxOf(bounds.outWidth, bounds.outHeight) > 0)
            }
            // Two distinct paths; neither is re-extracted on refresh.
            assertEquals(2, documents.epubReads)
            val tracked = cache.observeHistory(key).first()
            assertEquals(
                listOf("primary:Download/A.epub", "primary:Download/other/A.epub"),
                tracked.map { it.path },
            )
            assertEquals(listOf(2000L, 1000L), tracked.map { it.lastReadAt })
            assertTrue(tracked.map { it.coverUri }.distinct().size == 2)
            documents.archive = epub(legacy = true)
            documents.modified++
            cache.save(key, source.read(config), 2)
            assertEquals(4, documents.epubReads)
            assertTrue(
                tracked.first().coverUri != cache.observeHistory(key).first().first().coverUri,
            )
            documents.archive = epub(includeCover = false)
            documents.modified++
            val withoutCover = source.read(config)
            assertEquals("Actual title", withoutCover.current?.title)
            assertNull(withoutCover.current?.coverUri)
            documents.history = "return false"
            cache.save(key, source.read(config), 2)
            assertEquals(tracked.map { it.path }, cache.observeHistory(key).first().map { it.path })
            documents.history = "return {{file='/sdcard/Download/B.epub',time=4},false}"
            documents.missing = true
            val partial = source.read(config)
            cache.save(key, partial, 3)
            assertEquals(1, partial.skippedHistoryEntries)
            assertEquals(ReadingSyncIssue.InvalidData, partial.warning)
            val remaining = cache.observeHistory(key).first().single()
            assertEquals("primary:Download/B.epub", remaining.path)
            assertEquals(BookAvailability.Missing.name, remaining.availability)
            covers.clear()
            assertTrue(File(directory, "covers").listFiles().orEmpty().isEmpty())
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    private class Documents(var archive: ByteArray) : SourceDocumentReader {
        var percent = 0.7
        var history = """return {
            {file='/sdcard/Download/A.epub',time=2},
            {file='/sdcard/Download/A.epub',time=3},
            {file='/sdcard/Download/other/A.epub',time=1}}
        """
        var epubReads = 0
        var modified = 1L
        var missing = false
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            if (missing) throw MissingSourceFile()
            return SourceDocument(
                "content://test/" + path.joinToString("/"),
                "application/epub+zip",
                archive.size.toLong(),
                modified,
            )
        }
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            val bytes = when (path.last()) {
                "settings.reader.lua" -> "return {lastfile='/sdcard/Download/A.epub'}".toByteArray()

                "history.lua" -> history.toByteArray()

                "metadata.epub.lua" ->
                    "return {percent_finished=$percent,summary={status='reading'}}".toByteArray()

                else -> {
                    epubReads++
                    archive
                }
            }
            return bytes.inputStream().use { parse(it) }
        }
    }

    private fun workspace(prefix: String) =
        Files.createTempDirectory(context.cacheDir.toPath(), prefix).toFile()

    private fun epub(legacy: Boolean = false, includeCover: Boolean = true): ByteArray {
        val bitmap = Bitmap.createBitmap(1000, 1600, Bitmap.Config.ARGB_8888)
        val cover = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, cover)
        bitmap.recycle()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val container = """<container><rootfiles>
                <rootfile full-path='OPS/book.opf'/></rootfiles></container>"""
            val coverMeta = if (legacy) "<meta name='cover' content='cover'/>" else ""
            val coverProperty = if (legacy) "" else "properties='cover-image'"
            val opf = """<package><metadata><title>Actual title</title><creator>Author</creator>
                $coverMeta</metadata><manifest><item id='cover' $coverProperty href='cover.png'/>
                </manifest></package>"""
            val entries = mutableMapOf(
                "META-INF/container.xml" to container.toByteArray(),
                "OPS/book.opf" to opf.toByteArray(),
            )
            if (includeCover) entries["OPS/cover.png"] = cover.toByteArray()
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
