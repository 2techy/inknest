package dev.twotechy.inknest.data.source.stats

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.cancellableQuery
import dev.twotechy.inknest.data.source.copyBounded
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class StatisticsProbeResult(
    val books: Long,
    val pageRows: Long,
    val walPresent: Boolean,
)
internal class UnsupportedStatisticsSchema : IOException()

internal class StatisticsSnapshotProbe(
    private val files: SourceDocumentReader,
    private val cache: File,
) {
    private val mutex = Mutex()
    suspend fun read(tree: String): StatisticsProbeResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            var last: Exception? = null
            repeat(2) {
                currentCoroutineContext().ensureActive()
                val dir = Files.createTempDirectory(cache.toPath(), "reader-stats-").toFile()
                try {
                    return@withLock snapshot(tree, dir)
                } catch (error: SQLiteException) {
                    last = error
                } catch (error: SnapshotChanged) {
                    last = error
                } catch (
                    error: MissingSourceFile,
                ) {
                    throw error
                } catch (
                    error: UnsupportedStatisticsSchema,
                ) {
                    throw error
                } catch (
                    error: dev.twotechy.inknest.data.source.SourceLimitExceeded,
                ) {
                    throw error
                } catch (error: IOException) {
                    last = error
                } finally {
                    check(dir.canonicalFile.parentFile == cache.canonicalFile)
                    dir.deleteRecursively()
                }
            }
            if (last is SnapshotChanged) throw UnstableStatisticsSnapshot()
            if (last is SQLiteException) throw InvalidStatisticsSnapshot()
            throw IOException("Snapshot transport failed", last)
        }
    }

    private suspend fun snapshot(tree: String, dir: File): StatisticsProbeResult {
        val names = listOf("statistics.sqlite3", "statistics.sqlite3-wal", "statistics.sqlite3-shm")
        val before = names.associateWith { metadata(tree, it) }
        if (before[names[0]] == null) throw MissingSourceFile()
        copySnapshotFiles(tree, dir, before)
        val after = names.associateWith { metadata(tree, it) }
        if (before != after) throw SnapshotChanged()
        return readSnapshot(File(dir, names[0]), before[names[1]] != null)
    }

    private suspend fun copySnapshotFiles(
        tree: String,
        dir: File,
        metadata: Map<String, SourceDocument?>,
    ) {
        var remaining = 256L * 1024 * 1024
        metadata.filterValues { it != null }.keys.forEach { name ->
            try {
                files.read(tree, listOf("settings", name)) { input ->
                    File(dir, name).outputStream().use {
                        copyBounded(input, it, remaining)
                    }
                }
            } catch (_: MissingSourceFile) {
                throw SnapshotChanged()
            }
            remaining -= File(dir, name).length()
        }
    }

    private suspend fun readSnapshot(file: File, walPresent: Boolean): StatisticsProbeResult {
        currentCoroutineContext().ensureActive()
        return SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        ).use { db ->
            validateIntegrity(db)
            validateSchema(db)
            currentCoroutineContext().ensureActive()
            StatisticsProbeResult(count(db, "book"), count(db, "page_stat_data"), walPresent)
        }
    }

    private suspend fun validateIntegrity(db: SQLiteDatabase) {
        query(db, "PRAGMA quick_check") { cursor ->
            if (!cursor.moveToFirst()) throw SnapshotChanged()
            if (cursor.getString(0) != "ok") throw SnapshotChanged()
            if (cursor.moveToNext()) throw SnapshotChanged()
        }
    }

    private suspend fun validateSchema(db: SQLiteDatabase) {
        val required = mapOf(
            "book" to setOf("id", "title", "authors"),
            "page_stat_data" to setOf("id_book", "start_time", "duration", "page"),
        )
        for ((table, requiredColumns) in required) {
            if (!columns(
                    db,
                    table,
                ).containsAll(requiredColumns)
            ) {
                throw UnsupportedStatisticsSchema()
            }
        }
    }

    private suspend fun metadata(tree: String, name: String): SourceDocument? = try {
        files.describe(tree, listOf("settings", name))
    } catch (_: MissingSourceFile) {
        null
    }

    private suspend fun columns(db: SQLiteDatabase, table: String): Set<String> =
        query(db, "PRAGMA table_info($table)") { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    )
                }
            }
        }
    private suspend fun count(db: SQLiteDatabase, table: String): Long =
        query(db, "SELECT COUNT(*) FROM $table") {
            it.moveToFirst()
            it.getLong(0)
        }
    private suspend fun <T> query(db: SQLiteDatabase, sql: String, read: (Cursor) -> T): T =
        cancellableQuery { signal -> db.rawQuery(sql, null, signal).use(read) }

    private class SnapshotChanged : IOException()
}

internal class UnstableStatisticsSnapshot : IOException()
internal class InvalidStatisticsSnapshot : IOException()
