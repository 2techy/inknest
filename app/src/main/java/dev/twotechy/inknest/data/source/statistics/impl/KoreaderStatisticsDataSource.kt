package dev.twotechy.inknest.data.source.statistics.impl

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.data.source.statistics.InvalidStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.SnapshotFingerprint
import dev.twotechy.inknest.data.source.statistics.StatisticsBookIdentity
import dev.twotechy.inknest.data.source.statistics.StatisticsEventDataSource
import dev.twotechy.inknest.data.source.statistics.StatisticsEventReader
import dev.twotechy.inknest.data.source.statistics.StatisticsProbeResult
import dev.twotechy.inknest.data.source.statistics.StatisticsSnapshotChangedException
import dev.twotechy.inknest.data.source.statistics.UnstableStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.UnsupportedStatisticsSchemaException
import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.platform.tracing.OperationTracer
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

internal class KoreaderStatisticsDataSource(
    private val files: SourceDocumentReader,
    private val cache: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val tracer: OperationTracer = OperationTracer(),
) : StatisticsEventDataSource {
    private val mutex = Mutex()
    suspend fun read(tree: String): StatisticsProbeResult =
        readSnapshotData(StatisticsSnapshotFiles(files, tree)) { db, fingerprint ->
            probeStatisticsDatabase(db, fingerprint.hasWal)
        }

    override suspend fun fingerprint(tree: String): SnapshotFingerprint =
        withContext(ioDispatcher) {
            StatisticsSnapshotFiles(files, tree).fingerprint()
        }

    override suspend fun <T> withEvents(
        tree: String,
        consume: suspend (StatisticsEventReader) -> T,
    ): T = readSnapshotData(StatisticsSnapshotFiles(files, tree)) { db, snapshot ->
        consume(object : StatisticsEventReader {
            override val fingerprint = snapshot

            override suspend fun forEachBatch(consume: suspend (List<ReadingEvent>) -> Unit) =
                readStatisticsEvents(db, consume)
            override suspend fun forEachBookBatch(
                consume: suspend (List<StatisticsBookIdentity>) -> Unit,
            ) = readStatisticsBooks(db, consume)
        })
    }

    private suspend fun <T> readSnapshotData(
        source: StatisticsSnapshotFiles,
        read: suspend (SQLiteDatabase, SnapshotFingerprint) -> T,
    ): T = withContext(ioDispatcher) {
        mutex.withLock {
            var last: Exception? = null
            repeat(MAX_ATTEMPTS) { attempt ->
                delay(100L * attempt)
                currentCoroutineContext().ensureActive()
                val dir = Files.createTempDirectory(cache.toPath(), "reader-stats-").toFile()
                try {
                    val fingerprint = tracer.begin("InkNest.statistics.snapshot.copy").use {
                        source.copyTo(dir)
                    }
                    return@withLock readStatisticsDatabase(
                        File(dir, StatisticsSnapshotFiles.DATABASE_NAME),
                    ) { db -> read(db, fingerprint) }
                } catch (error: SQLiteException) {
                    last = error
                } catch (error: StatisticsSnapshotChangedException) {
                    last = error
                } catch (
                    error: MissingSourceFileException,
                ) {
                    throw error
                } catch (
                    error: UnsupportedStatisticsSchemaException,
                ) {
                    throw error
                } catch (
                    error: dev.twotechy.inknest.data.source.io.SourceLimitExceededException,
                ) {
                    throw error
                } catch (error: IOException) {
                    last = error
                } finally {
                    check(dir.canonicalFile.parentFile == cache.canonicalFile)
                    dir.deleteRecursively()
                }
            }
            if (last is StatisticsSnapshotChangedException) {
                throw UnstableStatisticsSnapshotException()
            }
            if (last is SQLiteException) throw InvalidStatisticsSnapshotException()
            throw IOException("Snapshot transport failed", last)
        }
    }
    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
