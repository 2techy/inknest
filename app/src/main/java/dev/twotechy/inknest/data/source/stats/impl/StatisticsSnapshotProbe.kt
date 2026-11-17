package dev.twotechy.inknest.data.source.stats.impl

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.stats.InvalidStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.StatisticsBookIdentity
import dev.twotechy.inknest.data.source.stats.StatisticsEventReader
import dev.twotechy.inknest.data.source.stats.StatisticsEventSource
import dev.twotechy.inknest.data.source.stats.StatisticsProbeResult
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotChanged
import dev.twotechy.inknest.data.source.stats.UnstableStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
import dev.twotechy.inknest.model.ReadingEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class StatisticsSnapshotProbe(
    private val files: SourceDocumentReader,
    private val cache: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StatisticsEventSource {
    private val mutex = Mutex()
    suspend fun read(tree: String): StatisticsProbeResult =
        readSnapshotData(StatisticsSnapshotFiles(files, tree)) { db, wal ->
            probeStatisticsDatabase(db, wal)
        }

    override suspend fun <T> withEvents(
        tree: String,
        consume: suspend (StatisticsEventReader) -> T,
    ): T = readSnapshotData(StatisticsSnapshotFiles(files, tree)) { db, _ ->
        consume(object : StatisticsEventReader {
            override suspend fun forEachBatch(consume: suspend (List<ReadingEvent>) -> Unit) =
                readStatisticsEvents(db, consume)
            override suspend fun forEachBookBatch(
                consume: suspend (List<StatisticsBookIdentity>) -> Unit,
            ) = readStatisticsBooks(db, consume)
        })
    }

    private suspend fun <T> readSnapshotData(
        source: StatisticsSnapshotFiles,
        read: suspend (SQLiteDatabase, Boolean) -> T,
    ): T = withContext(ioDispatcher) {
        mutex.withLock {
            var last: Exception? = null
            repeat(MAX_ATTEMPTS) { attempt ->
                delay(100L * attempt)
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
    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
