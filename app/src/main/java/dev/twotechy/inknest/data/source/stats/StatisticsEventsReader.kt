package dev.twotechy.inknest.data.source.stats

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.withCancellableSignal
import dev.twotechy.inknest.model.ReadingEvent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun readStatisticsEvents(
    db: SQLiteDatabase,
    consume: suspend (List<ReadingEvent>) -> Unit,
) {
    val context = currentCoroutineContext()
    withCancellableSignal { signal ->
        val layout = if (hasLayoutColumn(db)) "total_pages" else "NULL AS total_pages"
        db.rawQuery(
            "SELECT start_time, duration, id_book, page, $layout FROM page_stat_data",
            null,
            signal,
        ).use { cursor ->
            val batch = ArrayList<ReadingEvent>(StatisticsEventReader.BATCH_SIZE)
            var visited = 0
            while (cursor.moveToNext()) {
                context.ensureActive()
                if (++visited > 1_000_000) throw SourceLimitExceeded()
                cursor.readEvent()?.let { batch.add(it) }
                if (batch.size == StatisticsEventReader.BATCH_SIZE) {
                    consume(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) consume(batch.toList())
        }
    }
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

private fun hasLayoutColumn(db: SQLiteDatabase): Boolean =
    db.rawQuery("PRAGMA table_info(page_stat_data)", null).use { cursor ->
        var found = false
        while (cursor.moveToNext()) if (cursor.getString(1) == "total_pages") found = true
        found
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
