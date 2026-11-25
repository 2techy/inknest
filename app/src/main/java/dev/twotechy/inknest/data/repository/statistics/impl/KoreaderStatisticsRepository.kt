package dev.twotechy.inknest.data.repository.statistics.impl

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.database.statistics.StatisticsDao
import dev.twotechy.inknest.data.repository.shared.ObservationRetry
import dev.twotechy.inknest.data.repository.shared.recoverObservation
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.statistics.StatisticsRepository
import dev.twotechy.inknest.data.repository.statistics.miniStatistics
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.statistics.StatisticsEventDataSource
import dev.twotechy.inknest.data.source.statistics.UnsupportedStatisticsSchemaException
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatistics
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsDayBook
import dev.twotechy.inknest.model.StatisticsDayDetail
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.platform.clock.SystemStatisticsClock
import dev.twotechy.inknest.platform.tracing.OperationTracer
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class KoreaderStatisticsRepository(
    private val config: ReadingSourceConfigRepository,
    private val dao: StatisticsDao,
    source: StatisticsEventDataSource,
    private val scope: CoroutineScope,
    transition: Mutex = Mutex(),
    private val clock: StatisticsClock = SystemStatisticsClock(scope),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    permissionSnapshot: suspend () -> CoroutineContext = { EmptyCoroutineContext },
    tracer: OperationTracer = OperationTracer(),
) : StatisticsRepository {
    private val importer =
        StatisticsImporter(config, dao, source, transition, permissionSnapshot, tracer = tracer) {
            clock.moments.value.zone
        }
    private val requests = Mutex()
    private var request: Pair<String, Deferred<Unit>>? = null
    private val status =
        MutableStateFlow<Pair<String?, ReadingSyncStatus>>(null to ReadingSyncStatus())
    private val observationRetry = ObservationRetry()
    private val dayRetry = ObservationRetry()
    private val readError = MutableStateFlow<String?>(null)
    private val tree = config.config.map { it.koreaderTreeUri }.distinctUntilChanged()
    override val data = tree.flatMapLatest { selected ->
        if (selected == null) {
            flowOf(StatisticsData())
        } else {
            combine(
                dao.observeSnapshot(selected).recoverObservation(
                    observationRetry,
                    onError = { readError.value = selected },
                    onSuccess = {
                        if (readError.value == selected) readError.value = null
                    },
                ).onStart { emit(null) }.map { snapshot ->
                    StatisticsData(
                        days = snapshot?.days.orEmpty().associate {
                            LocalDate.ofEpochDay(it.day) to StatisticsAmount(it.seconds, it.pages)
                        },
                        generation = snapshot?.source?.generation,
                        zone = snapshot?.source?.zone?.let(ZoneId::of),
                        configured = true,
                        sourceTree = selected,
                        updatedAt = snapshot?.source?.updatedAt,
                    )
                },
                status,
                readError,
            ) { cached, (key, sync), failed ->
                val current = if (key == selected) sync else ReadingSyncStatus()
                val recovered = if (failed == selected) {
                    current.copy(
                        error = ReadingSyncIssue.Cache,
                        stale = cached.updatedAt != null,
                    )
                } else {
                    current
                }
                cached.copy(sync = recovered)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, StatisticsData())

    override val stats = combine(data, clock.moments) { data, (today, _) ->
        data.updatedAt?.let {
            val (daily, streak) = miniStatistics(data.days, today)
            ReadingStatistics(daily, streak)
        }
    }.flowOn(defaultDispatcher)

    override fun observeDay(day: LocalDate, page: Int): Flow<StatisticsDayDetail> {
        require(page >= 0)
        val selection = combine(data, config.config, clock.moments) { cached, selected, moment ->
            if (day > moment.today) null else cached.daySelection(selected, moment.zone)
        }.distinctUntilChanged()
        return selection.flatMapLatest { selected ->
            val details = if (selected == null) {
                flowOf(StatisticsDayDetail(loading = false))
            } else {
                observeCachedDay(selected, day, page)
            }
            combine(details, data) { detail, cached ->
                if (selected != null && cached.generation != selected.generation) {
                    StatisticsDayDetail()
                } else {
                    detail.copy(
                        sync = cached.sync,
                        loading = detail.loading || (detail.amount == null && cached.sync.running),
                    )
                }
            }
        }.buffer(0).flowOn(defaultDispatcher)
    }

    override fun retryDay() {
        dayRetry.retry()
        observationRetry.retry()
        config.retryRead()
    }

    private fun StatisticsData.daySelection(
        selected: ReadingSourceConfig,
        zone: ZoneId,
    ): DaySelection? {
        val source = selected.koreaderTreeUri ?: return null
        val generation = generation ?: return null
        if (sourceTree != source || this.zone != zone) return null
        return DaySelection(source, generation, selected.sourceId, selected.booksTreeUris)
    }

    private fun observeCachedDay(
        selected: DaySelection,
        day: LocalDate,
        page: Int,
    ): Flow<StatisticsDayDetail> {
        val failed = MutableStateFlow(false)
        val snapshots = dao.observeDayDetail(
            selected.source,
            selected.generation,
            day.toEpochDay(),
            selected.fileSource,
            selected.libraryFolders,
            DAY_BOOK_PAGE_SIZE + 1,
            page.toLong() * DAY_BOOK_PAGE_SIZE,
        ).map { rows ->
            val first = rows.firstOrNull() ?: return@map StatisticsDayDetail()
            StatisticsDayDetail(
                amount = StatisticsAmount(first.daySeconds, first.dayPages),
                books = rows.take(DAY_BOOK_PAGE_SIZE).mapNotNull { row ->
                    row.bookId?.let {
                        StatisticsDayBook(
                            it,
                            row.title,
                            StatisticsAmount(checkNotNull(row.bookSeconds), row.bookPages),
                            row.libraryPath?.let { path ->
                                selected.fileSource?.let { source -> ReadingItemId(source, path) }
                            },
                        )
                    }
                },
                hasMore = rows.size > DAY_BOOK_PAGE_SIZE,
                loading = false,
            )
        }.recoverObservation(
            dayRetry,
            onError = { failed.value = true },
            onSuccess = { failed.value = false },
        ).onStart { emit(StatisticsDayDetail()) }
        return combine(snapshots, failed) { snapshot, error -> snapshot.copy(cacheError = error) }
    }

    private data class DaySelection(
        val source: String,
        val generation: String,
        val fileSource: String?,
        val libraryFolders: Set<String>,
    )

    init {
        scope.launch {
            tree.onEach { selected ->
                requests.withLock {
                    if (request?.first != selected) request?.second?.cancel()
                }
            }.drop(1).collectLatest { selected ->
                if (selected != null) refreshAutomatically()
            }
        }
    }

    init {
        scope.launch {
            combine(tree, data, clock.moments) { selected, cached, (_, zone) ->
                Triple(selected, cached.zone, zone)
            }.distinctUntilChanged()
                .collectLatest { (selected, cachedZone, zone) ->
                    if (selected != null && cachedZone != zone) rebin(selected, zone)
                }
        }
    }

    private suspend fun rebin(tree: String, zone: ZoneId) {
        try {
            importer.rebin(tree, zone)
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
        } catch (error: Exception) {
            status.value = tree to ReadingSyncStatus(error = statisticsIssue(error), stale = true)
        }
    }

    private suspend fun refreshAutomatically() {
        try {
            refresh()
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
        }
    }

    override suspend fun refresh() {
        observationRetry.retry()
        config.retryRead()
        val selected = config.config.first().koreaderTreeUri ?: return
        val job = requests.withLock {
            request?.takeIf { it.first == selected && it.second.isActive }?.second
                ?: scope.async(start = CoroutineStart.LAZY) { load(selected) }.also {
                    request = selected to it
                    it.start()
                }
        }
        job.await()
    }

    private suspend fun load(tree: String) {
        val attempt = System.currentTimeMillis()
        status.value = tree to ReadingSyncStatus(lastAttemptAt = attempt, running = true)
        try {
            importer.refresh(tree)
            status.value = tree to ReadingSyncStatus(lastAttemptAt = attempt)
        } catch (cancelled: CancellationException) {
            status.value = tree to ReadingSyncStatus(lastAttemptAt = attempt)
            throw cancelled
        } catch (error: Exception) {
            val issue = statisticsIssue(error)
            status.value = tree to ReadingSyncStatus(
                lastAttemptAt = attempt,
                error = issue,
                stale = true,
            )
        }
    }
}

private const val DAY_BOOK_PAGE_SIZE = 30

private fun statisticsIssue(error: Exception): ReadingSyncIssue = when (error) {
    is SecurityException -> ReadingSyncIssue.PermissionLost
    is MissingSourceFileException -> ReadingSyncIssue.MissingData
    is UnsupportedStatisticsSchemaException, is SQLiteException -> ReadingSyncIssue.InvalidData
    is IOException -> ReadingSyncIssue.Provider
    else -> throw error
}
