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
        if (marker.isFile && marker.readText() == BenchmarkFixture.VERSION) return
        val downloads = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
        check(directory.canonicalFile == File(downloads, "InkNestBenchmark"))
        if (directory.exists()) check(directory.deleteRecursively())
        check(directory.mkdirs() || directory.isDirectory)
        File(directory, "books/a/b/c").apply { check(mkdirs() || isDirectory) }
        repeat(BenchmarkFixture.BOOKS) { index -> writeBook(book(index), index) }
        writeReading(changed = false)
        writeStatistics()
        marker.writeText(BenchmarkFixture.VERSION)
    }

    private fun book(index: Int) = File(directory, "books/a/b/c/book-%04d.epub".format(index))

    private fun writeReading(changed: Boolean) {
        val current = book(0).absolutePath
        File(directory, "settings.reader.lua").writeText("""return { lastfile = "$current" }""")
        val time = if (changed) 1793534460 else 1793534400
        val history = (0 until BenchmarkFixture.HISTORY).joinToString(
            prefix = "return {\n",
            postfix = "\n}",
            separator = ",\n",
        ) { "{ file = \"${book(it).absolutePath}\", time = ${time - it} }" }
        File(directory, "history.lua").writeText(history)
        repeat(if (changed) 1 else BenchmarkFixture.HISTORY) { index ->
            val sidecar = File(book(index).path.removeSuffix(".epub") + ".sdr")
                .apply { check(mkdirs() || isDirectory) }
            val progress = if (changed && index == 0) 0.38 else 0.37
            File(sidecar, "metadata.epub.lua").writeText(
                """return { percent_finished = $progress, doc_pages = 300, summary = { status = "reading" } }""",
            )
        }
    }

    fun update(changed: Boolean) {
        writeReading(changed)
        SQLiteDatabase.openDatabase(
            File(directory, "settings/statistics.sqlite3").path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.execSQL("DELETE FROM page_stat_data WHERE start_time > 1793534400")
            if (changed) {
                db.execSQL("INSERT INTO page_stat_data VALUES(1, 1793534460, 77, 301, 400)")
            }
        }
    }

    private fun writeBook(file: File, index: Int) {
        val title = "Benchmark Book %04d".format(index + 1)
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
                    repeat(BenchmarkFixture.BOOKS) {
                        book.bindLong(1, (it + 1).toLong())
                        book.bindString(2, "Benchmark Book %04d".format(it + 1))
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
            repeat(BenchmarkFixture.EVENTS) { index ->
                event.bindLong(1, (index % BenchmarkFixture.BOOKS + 1).toLong())
                event.bindLong(2, anchor - (index % 180) * 86_400L)
                event.bindLong(3, (15 + index % 75).toLong())
                event.bindLong(4, (index % 300 + 1).toLong())
                event.executeInsert()
            }
        }
    }
}
