package dev.twotechy.inknest.benchmarkfixture

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Synthetic files exercise production SAF/Lua/EPUB/SQLite readers; no runtime repository is replaced.
internal class BenchmarkDataset {
    val directory =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "InkNestBenchmark",
        )
    val documentId = "primary:Download/InkNestBenchmark"
    private val marker get() = File(directory, "dataset-version")

    fun ensure() {
        if (marker.isFile && marker.readText() == VERSION) return
        check(directory.mkdirs() || directory.isDirectory)
        val books = File(directory, "books").apply { check(mkdirs() || isDirectory) }
        repeat(BOOKS) { index -> writeBook(File(books, "book-%03d.epub".format(index)), index) }
        val current = File(books, "book-000.epub").absolutePath
        File(directory, "settings.reader.lua").writeText("""return { lastfile = "$current" }""")
        File(
            directory,
            "history.lua",
        ).writeText("""return { { file = "$current", time = 1793534400 } }""")
        val sidecar = File(books, "book-000.sdr").apply { mkdirs() }
        File(
            sidecar,
            "metadata.epub.lua",
        ).writeText("""return { percent_finished = 0.37, summary = { status = "reading" } }""")
        writeStatistics()
        marker.writeText(VERSION)
    }

    private fun writeBook(file: File, index: Int) {
        val title = "Benchmark Book %03d".format(index + 1)
        val container = """<container><rootfiles>
            <rootfile full-path="OPS/book.opf"/>
            </rootfiles></container>"""
        val opf = """<package xmlns:dc="http://purl.org/dc/elements/1.1/">
            <metadata><dc:title>$title</dc:title><dc:creator>Benchmark Author</dc:creator></metadata>
            <manifest><item id="cover" href="cover.png"
            properties="cover-image" media-type="image/png"/>
            <item id="text" href="chapter.xhtml" media-type="application/xhtml+xml"/></manifest>
            <spine><itemref idref="text"/></spine></package>"""
        val chapter = """<html><body><h1>$title</h1>
            <p>Synthetic benchmark text.</p></body></html>"""
        ZipOutputStream(file.outputStream()).use { zip ->
            val entries = mapOf(
                "mimetype" to "application/epub+zip".toByteArray(),
                "META-INF/container.xml" to container.toByteArray(),
                "OPS/book.opf" to opf.toByteArray(),
                "OPS/chapter.xhtml" to chapter.toByteArray(),
                "OPS/cover.png" to cover(index),
            )
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun cover(index: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(240, 336, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(220 - index % 50, 220 - index % 50, 220 - index % 50))
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 38f
                isAntiAlias = true
            }
            canvas.drawText("INK", 24f, 90f, paint)
            canvas.drawText("%03d".format(index + 1), 24f, 160f, paint)
            return ByteArrayOutputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                it.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun writeStatistics() {
        val settings = File(directory, "settings").apply { mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(File(settings, "statistics.sqlite3"), null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS book(id INTEGER PRIMARY KEY, title TEXT, authors TEXT)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS page_stat_data(id_book INTEGER, start_time INTEGER, duration INTEGER, page INTEGER, total_pages INTEGER)",
            )
            db.beginTransaction()
            try {
                db.execSQL("DELETE FROM page_stat_data")
                db.execSQL("DELETE FROM book")
                db.compileStatement(
                    "INSERT INTO book VALUES(?, ?, 'Benchmark Author')",
                ).use { book ->
                    repeat(BOOKS) {
                        book.bindLong(1, (it + 1).toLong())
                        book.bindString(2, "Benchmark Book %03d".format(it + 1))
                        book.executeInsert()
                    }
                }
                insertEvents(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun insertEvents(db: SQLiteDatabase) {
        val anchor = Instant.parse("2026-11-01T12:00:00Z").epochSecond
        db.compileStatement("INSERT INTO page_stat_data VALUES(?, ?, ?, ?, 300)").use { event ->
            repeat(EVENTS) { index ->
                event.bindLong(1, (index % BOOKS + 1).toLong())
                event.bindLong(2, anchor - (index % 180) * 86_400L)
                event.bindLong(3, (15 + index % 75).toLong())
                event.bindLong(4, (index % 300 + 1).toLong())
                event.executeInsert()
            }
        }
    }

    companion object {
        const val BOOKS = 100
        const val EVENTS = 50_000
        private const val VERSION = "1:100:50000:2026-11-01"
    }
}
