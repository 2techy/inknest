package dev.twotechy.inknest.data.source.stats

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.ReadingEvent
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
    suspend fun read(tree: String): StatisticsProbeResult =
        readSnapshotData(StatisticsSnapshotFiles(files, tree)) { db, wal ->
            probeStatisticsDatabase(db, wal)
        }

    suspend fun events(tree: String): List<ReadingEvent> =
        readSnapshotData(StatisticsSnapshotFiles(files, tree)) {
                db,
                _,
            ->
            readStatisticsEvents(db)
        }

    private suspend fun <T> readSnapshotData(
        source: StatisticsSnapshotFiles,
        read: suspend (SQLiteDatabase, Boolean) -> T,
    ): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            var last: Exception? = null
            repeat(2) {
                currentCoroutineContext().ensureActive()
                val dir = Files.createTempDirectory(cache.toPath(), "reader-stats-").toFile()
                try {
                    val wal = source.copyTo(dir)
                    return@withLock readStatisticsDatabase(
                        File(dir, StatisticsSnapshotFiles.DATABASE_NAME),
                    ) { db -> read(db, wal) }
                } catch (error: SQLiteException) {
                    last = error
                } catch (error: StatisticsSnapshotChanged) {
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
            if (last is StatisticsSnapshotChanged) throw UnstableStatisticsSnapshot()
            if (last is SQLiteException) throw InvalidStatisticsSnapshot()
            throw IOException("Snapshot transport failed", last)
        }
    }
}

internal class UnstableStatisticsSnapshot : IOException()
internal class InvalidStatisticsSnapshot : IOException()
