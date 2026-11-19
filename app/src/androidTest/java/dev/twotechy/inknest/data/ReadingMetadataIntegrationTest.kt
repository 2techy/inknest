package dev.twotechy.inknest.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.net.toUri
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.source.books.BookMetadataEnricher
import dev.twotechy.inknest.data.source.books.FileBookCoverStore
import dev.twotechy.inknest.data.source.books.impl.BookProbeDataSource
import dev.twotechy.inknest.data.source.koreader.impl.KoreaderCurrentReadingDataSource
import dev.twotechy.inknest.data.source.saf.MissingSourceFile
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.platform.AndroidBookCoverDecoder
import dev.twotechy.inknest.platform.BookCoverRequest
import dev.twotechy.inknest.platform.BookCoverSize
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
        booksTreeUris = setOf(
            "content://com.android.externalstorage.documents/tree/primary%3ADownload",
        ),
    )

    @Test fun metadataProgressHistoryAndCacheUseExactIdentity() = runBlocking {
        val directory = workspace("metadata-test-")
        val database = Room.inMemoryDatabaseBuilder<ReadingDatabase>(context)
            .setDriver(AndroidSQLiteDriver()).build()
        try {
            val cache = ReadingCache(database.readingDao())
            val covers = FileBookCoverStore(File(directory, "covers"))
            val documents = Documents(epub())
            val books = BookProbeDataSource(documents, directory)
            val source = KoreaderCurrentReadingDataSource(
                documents,
                books,
                metadataReader = BookMetadataEnricher(books, covers, cache::metadata),
            )
            val key = checkNotNull(config.readingCacheKey())
            for ((percent, expectedPage) in listOf(0.7 to 700, 0.3 to 300)) {
                documents.percent = percent
                val snapshot = source.read(config)
                cache.save(key, snapshot, 1)
                val item = checkNotNull(snapshot.current)
                assertEquals((percent * 100).toInt(), item.progress)
                val cached = checkNotNull(cache.observe(key).first()).book
                assertEquals(expectedPage, cached?.currentPage)
                assertEquals(1000, cached?.totalPages)
                assertBookMetadata(item)
            }
            // Two distinct paths; neither is re-extracted on refresh.
            assertEquals(2, documents.epubReads)
            val tracked = cache.observeHistory(key).first()
            assertEquals(
                listOf("primary:Download/A.epub", "primary:Download/other/A.epub"),
                tracked.map { it.path },
            )
            assertEquals(listOf(2000L, 1000L), tracked.map { it.lastReadAt })
            assertEquals(listOf(300, 600), tracked.map { it.currentPage })
            assertEquals(listOf(1000, 2000), tracked.map { it.totalPages })
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

    private suspend fun assertBookMetadata(item: ReadingItem) {
        assertEquals("Actual title", item.title)
        assertEquals("Author", item.author)
        assertEquals(ReadingStatus.Reading, item.status)
        val file = File(checkNotNull(checkNotNull(item.coverUri).toUri().path))
        assertEquals("webp", file.extension)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= 480)
        assertTrue(maxOf(bounds.outWidth, bounds.outHeight) > 0)
        assertEquals("image/webp", bounds.outMimeType)
        val decoder = AndroidBookCoverDecoder(checkNotNull(file.parentFile))
        val request =
            BookCoverRequest(checkNotNull(item.coverUri), BookCoverSize.forBounds(120, 180))
        val color = checkNotNull(decoder.load(request))
        val eink = checkNotNull(decoder.load(request.copy(color = false)))
        try {
            val bitmap = color.image.asAndroidBitmap()
            val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            assertTrue(Color.red(pixel) > Color.green(pixel) + 100)
            assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
            assertEquals(Bitmap.Config.RGB_565, eink.image.asAndroidBitmap().config)
            assertTrue(eink.byteCount < color.byteCount)
        } finally {
            color.image.asAndroidBitmap().recycle()
            eink.image.asAndroidBitmap().recycle()
        }
    }

    @Test fun legacyPngIsReExtractedFromTheBookAndSavedAsWebp() = runBlocking {
        val directory = workspace("legacy-cover-test-")
        val database = Room.inMemoryDatabaseBuilder<ReadingDatabase>(context)
            .setDriver(AndroidSQLiteDriver()).build()
        try {
            val cache = ReadingCache(database.readingDao())
            val coverDirectory = File(directory, "covers").apply { mkdirs() }
            val covers = FileBookCoverStore(coverDirectory)
            val documents = Documents(epub())
            val books = BookProbeDataSource(documents, directory)
            val source = KoreaderCurrentReadingDataSource(
                documents,
                books,
                metadataReader = BookMetadataEnricher(books, covers, cache::metadata),
            )
            val key = checkNotNull(config.readingCacheKey())
            val original = source.read(config)
            val legacy = Uri.fromFile(
                File(coverDirectory, "legacy.png").apply {
                    writeBytes(byteArrayOf(0, 1, 2))
                },
            ).toString()
            cache.save(
                key,
                original.copy(
                    current = original.current?.copy(coverUri = legacy),
                    history = original.history?.map { it.copy(coverUri = legacy) },
                    metadata = original.metadata.mapValues { it.value.copy(coverUri = legacy) },
                ),
                1,
            )
            val reads = documents.epubReads
            val refreshed = source.read(config)
            assertEquals(reads + 2, documents.epubReads)
            cache.save(key, refreshed, 2)
            val persisted = cache.observeHistory(key).first()
            assertEquals(2, persisted.size)
            assertTrue(persisted.all { checkNotNull(it.coverUri).endsWith(".webp") })
            source.read(config)
            assertEquals(reads + 2, documents.epubReads)
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
                    (
                        "return {percent_finished=$percent,doc_pages=" +
                            (if ("other" in path) 2000 else 1000) +
                            ",last_xpointer='/body',summary={status='reading'}}"
                        ).toByteArray()

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
        bitmap.eraseColor(Color.rgb(220, 30, 50))
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
