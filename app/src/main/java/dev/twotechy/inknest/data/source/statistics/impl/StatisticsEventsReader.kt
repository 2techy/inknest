package dev.twotechy.inknest.data.source.statistics.impl

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dev.twotechy.inknest.data.source.io.SourceLimitExceededException
import dev.twotechy.inknest.data.source.io.withCancellableSignal
import dev.twotechy.inknest.data.source.statistics.StatisticsBookIdentity
import dev.twotechy.inknest.data.source.statistics.StatisticsEventReader
import dev.twotechy.inknest.model.ReadingEvent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun readStatisticsEvents(
    db: SQLiteDatabase,
    consume: suspend (List<ReadingEvent>) -> Unit,
) {
    val layout = if (hasColumn(db, "page_stat_data", "total_pages")) {
        "total_pages"
    } else {
        "NULL AS total_pages"
    }
    readStatisticsRows(
        db,
        "SELECT start_time, duration, id_book, page, $layout FROM page_stat_data",
        Cursor::readEvent,
        consume,
    )
}

internal suspend fun readStatisticsBooks(
    db: SQLiteDatabase,
    consume: suspend (List<StatisticsBookIdentity>) -> Unit,
) {
    if (!hasColumn(db, "book", "id")) return
    val checksum = if (hasColumn(db, "book", "md5")) "md5" else "NULL"
    val title = if (hasColumn(db, "book", "title")) "title" else "NULL"
    readStatisticsRows(db, "SELECT id, $checksum, $title FROM book", Cursor::bookIdentity, consume)
}

// Both tables share cancellation, row limits and bounded batch ownership.
private suspend fun <T> readStatisticsRows(
    db: SQLiteDatabase,
    sql: String,
    read: Cursor.() -> T?,
    consume: suspend (List<T>) -> Unit,
) {
    val context = currentCoroutineContext()
    withCancellableSignal { signal ->
        db.rawQuery(sql, null, signal).use { cursor ->
            val batch = ArrayList<T>(StatisticsEventReader.BATCH_SIZE)
            var visited = 0
            while (cursor.moveToNext()) {
                context.ensureActive()
                if (++visited > 1_000_000) throw SourceLimitExceededException()
                cursor.read()?.let(batch::add)
                if (batch.size == StatisticsEventReader.BATCH_SIZE) {
                    consume(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) consume(batch.toList())
        }
    }
}

private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
    db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
        var found = false
        while (cursor.moveToNext()) if (cursor.getString(1) == column) found = true
        found
    }

private fun Cursor.readEvent(): ReadingEvent? {
    // SQLite is dynamically typed. Never coerce text/null into a successful zero-duration record.
    if ((0..2).any { !numericColumn(it) }) return null
    val start = getDouble(0)
    val duration = getDouble(1)
    val book = getDouble(2)
    if (!validInteger(start, 253402300799.0)) return null
    if (!validInteger(duration, 2147483647.0)) return null
    if (!validInteger(book, 9007199254740991.0)) return null
    return ReadingEvent(
        start.toLong() * 1000,
        duration.toLong(),
        book.toLong(),
        positivePage(3),
        positivePage(4),
    )
}

private fun Cursor.positivePage(index: Int): Long? {
    if (!numericColumn(index)) return null
    val value = getDouble(index)
    return value.takeIf { it > 0 && validInteger(it, Int.MAX_VALUE.toDouble()) }?.toLong()
}

private fun Cursor.numericColumn(index: Int) =
    getType(index) == Cursor.FIELD_TYPE_INTEGER || getType(index) == Cursor.FIELD_TYPE_FLOAT

private fun validInteger(value: Double, max: Double): Boolean =
    value.isFinite() && value >= 0 && value <= max && value == value.toLong().toDouble()

private val BOOK_CHECKSUM = Regex("[0-9a-f]{32}")

private fun Cursor.bookIdentity(): StatisticsBookIdentity? {
    if (getType(0) != Cursor.FIELD_TYPE_INTEGER) return null
    val checksum = getString(1)?.lowercase()?.takeIf(BOOK_CHECKSUM::matches)
    val title = if (getType(2) == Cursor.FIELD_TYPE_STRING) {
        getString(2).trim().take(1_024).takeIf(String::isNotEmpty)
    } else {
        null
    }
    return StatisticsBookIdentity(getLong(0), checksum, title)
}
