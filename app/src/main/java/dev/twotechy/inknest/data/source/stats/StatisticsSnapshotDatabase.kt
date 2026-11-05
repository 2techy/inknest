package dev.twotechy.inknest.data.source.stats

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dev.twotechy.inknest.data.source.cancellableQuery
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private enum class StatisticsTable(val nameInDatabase: String, val requiredColumns: Set<String>) {
    Book("book", setOf("id", "title", "authors")),
    Page("page_stat_data", setOf("id_book", "start_time", "duration", "page")),
}

internal suspend fun <T> readStatisticsDatabase(
    file: File,
    read: suspend (SQLiteDatabase) -> T,
): T {
    currentCoroutineContext().ensureActive()
    return SQLiteDatabase.openDatabase(
        file.path,
        null,
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ).use { db ->
        validateIntegrity(db)
        validateSchema(db)
        currentCoroutineContext().ensureActive()
        read(db)
    }
}

internal suspend fun probeStatisticsDatabase(db: SQLiteDatabase, wal: Boolean) =
    StatisticsProbeResult(count(db, StatisticsTable.Book), count(db, StatisticsTable.Page), wal)

private suspend fun validateIntegrity(db: SQLiteDatabase) {
    query(db, "PRAGMA quick_check") { cursor ->
        if (!cursor.moveToFirst()) throw StatisticsSnapshotChanged()
        if (cursor.getString(0) != "ok") throw StatisticsSnapshotChanged()
        if (cursor.moveToNext()) throw StatisticsSnapshotChanged()
    }
}

private suspend fun validateSchema(db: SQLiteDatabase) {
    for (table in StatisticsTable.entries) {
        if (!columns(
                db,
                table,
            ).containsAll(table.requiredColumns)
        ) {
            throw UnsupportedStatisticsSchema()
        }
    }
}

private suspend fun columns(db: SQLiteDatabase, table: StatisticsTable): Set<String> =
    query(db, "PRAGMA table_info(${table.nameInDatabase})") { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
    }

private suspend fun count(db: SQLiteDatabase, table: StatisticsTable): Long =
    query(db, "SELECT COUNT(*) FROM ${table.nameInDatabase}") {
        it.moveToFirst()
        it.getLong(0)
    }

private suspend fun <T> query(db: SQLiteDatabase, sql: String, read: (Cursor) -> T): T =
    cancellableQuery { signal -> db.rawQuery(sql, null, signal).use(read) }
