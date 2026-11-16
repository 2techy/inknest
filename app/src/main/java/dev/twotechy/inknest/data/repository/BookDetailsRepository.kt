package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.LibraryDao
import dev.twotechy.inknest.data.local.reading.StatisticsDao
import dev.twotechy.inknest.data.source.BookChecksum
import dev.twotechy.inknest.data.source.BookChecksumSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.BookReadingDay
import dev.twotechy.inknest.model.BookReadingStatistics
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.platform.clock.StatisticsClock
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

interface BookDetailsRepository {
    fun observe(id: ReadingItemId, historyLimit: Int): Flow<BookDetails>
    fun retry()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class CachedBookDetailsRepository(
    private val library: LibraryRepository,
    private val config: ReadingSourceConfigRepository,
    private val libraryDao: LibraryDao,
    private val statisticsDao: StatisticsDao,
    private val statistics: StatisticsRepository,
    private val checksumSource: BookChecksumSource,
    private val clock: StatisticsClock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BookDetailsRepository {
    private val retry = ObservationRetry()
    private val requests = MutableStateFlow(0L)
    override fun retry() {
        retry.retry()
        requests.update { it + 1 }
    }

    override fun observe(id: ReadingItemId, historyLimit: Int): Flow<BookDetails> {
        val failed = MutableStateFlow(false)
        val identity = combine(
            config.config,
            requests,
            libraryDao.observeFingerprint(id.sourceId, id.path).recoverObservation(
                retry,
                onError = { failed.value = true },
                onSuccess = { failed.value = false },
            ).onStart { emit(null) },
        ) { selected, request, fingerprint -> Triple(selected, request, fingerprint) }
            .distinctUntilChanged().flatMapLatest { (selected, _, _) ->
                identity(selected, id).recoverObservation(
                    retry,
                    onError = { failed.value = true },
                    onSuccess = { failed.value = false },
                ).onStart { emit(Identity()) }
            }
        val details = identity.flatMapLatest { identity ->
            combine(
                library.catalog,
                if (identity.finished) {
                    bookStatistics(
                        identity.checksum,
                        historyLimit,
                    )
                } else {
                    flowOf(BookReadingStatistics())
                },
            ) { catalog, stats ->
                val book = catalog.books.find { it.id == id }
                BookDetails(
                    book = book?.copy(availability = identity.availability ?: book.availability),
                    loading = catalog.loading,
                    statistics = stats,
                )
            }
        }
        return combine(details, failed) { snapshot, error ->
            if (error) {
                snapshot.copy(
                    statistics = snapshot.statistics.copy(error = ReadingSyncIssue.Cache),
                )
            } else {
                snapshot
            }
        }.flowOn(ioDispatcher)
    }

    private fun identity(selected: ReadingSourceConfig, id: ReadingItemId): Flow<Identity> = flow {
        if (selected.sourceId != id.sourceId) {
            emit(Identity(availability = BookAvailability.Unresolved, finished = true))
            return@flow
        }
        try {
            val checksum = checksumSource.checksum(selected, id.path) { tree, fingerprint ->
                libraryDao.checksum(id.sourceId, tree, id.path, fingerprint)
            }
            currentCoroutineContext().ensureActive()
            if (config.config.first() != selected) return@flow
            checksum.fingerprint?.let {
                // UPDATE only: a cleared inventory cannot be resurrected by a pending read.
                libraryDao.saveChecksum(id.sourceId, checksum.tree, id.path, it, checksum.checksum)
            }
            emit(Identity(checksum, BookAvailability.Available))
        } catch (_: SecurityException) {
            emit(Identity(availability = BookAvailability.PermissionLost, finished = true))
        } catch (_: MissingSourceFile) {
            emit(Identity(availability = BookAvailability.Missing, finished = true))
        } catch (_: IOException) {
            emit(Identity(finished = true))
        }
    }

    private fun bookStatistics(checksum: BookChecksum?, limit: Int): Flow<BookReadingStatistics> {
        val selection = statistics.data.map {
            StatisticsSelection(it.configured, it.generation, it.sourceTree, it.zone)
        }.distinctUntilChanged()
        val snapshots = combine(selection, clock.moments, config.config) { data, moment, selected ->
            Triple(data, moment, selected)
        }.flatMapLatest { (data, moment, selected) ->
            val generation = data.generation
            when {
                !data.configured || generation == null -> flowOf(
                    BookReadingStatistics(status = BookStatisticsStatus.Unavailable),
                )

                data.sourceTree != selected.koreaderTreeUri -> flowOf(BookReadingStatistics())

                checksum == null -> flowOf(
                    BookReadingStatistics(status = BookStatisticsStatus.Unlinked),
                )

                data.zone != moment.zone -> flowOf(BookReadingStatistics())

                else -> observeStatistics(
                    generation,
                    checksum.checksum,
                    moment.today.toEpochDay(),
                    limit,
                )
            }
        }
        // A refresh/error changes notices, not the cached per-book query or visible totals.
        return combine(snapshots, statistics.data) { snapshot, data ->
            snapshot.copy(error = data.sync.error ?: snapshot.error)
        }
    }

    private data class StatisticsSelection(
        val configured: Boolean,
        val generation: String?,
        val sourceTree: String?,
        val zone: java.time.ZoneId?,
    )

    private fun observeStatistics(
        generation: String,
        checksum: String,
        today: Long,
        limit: Int,
    ): Flow<BookReadingStatistics> {
        val failed = MutableStateFlow(false)
        val values = combine(
            statisticsDao.observeMatches(generation, checksum),
            statisticsDao.observeBookTotals(generation, checksum, today),
            statisticsDao.observeBookDays(generation, checksum, today, limit + 1),
        ) { matches, totals, days ->
            BookReadingStatistics(
                status = if (matches ==
                    0
                ) {
                    BookStatisticsStatus.Unlinked
                } else {
                    BookStatisticsStatus.Ready
                },
                seconds = totals.seconds,
                pages = totals.pages,
                readingDays = totals.days,
                firstRead = totals.firstDay?.let(LocalDate::ofEpochDay),
                lastRead = totals.lastDay?.let(LocalDate::ofEpochDay),
                history = days.take(limit).map {
                    BookReadingDay(LocalDate.ofEpochDay(it.day), it.seconds, it.pages)
                },
                hasMore = days.size > limit,
            )
        }.recoverObservation(retry, onError = { failed.value = true }, onSuccess = {
            failed.value =
                false
        })
            .onStart { emit(BookReadingStatistics()) }
        return combine(values, failed) { snapshot, error ->
            snapshot.copy(error = if (error) ReadingSyncIssue.Cache else null)
        }
    }

    private data class Identity(
        val checksum: BookChecksum? = null,
        val availability: BookAvailability? = null,
        val finished: Boolean = checksum !=
            null,
    )
}
