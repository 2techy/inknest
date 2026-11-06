package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.StatisticsDao
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.stats.StatisticsEventSource
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
import dev.twotechy.inknest.model.ReadingStats
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsData
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class KoreaderStatisticsRepository(
    private val config: ReadingSourceConfigRepository,
    private val dao: StatisticsDao,
    source: StatisticsEventSource,
    private val scope: CoroutineScope,
    transition: Mutex = Mutex(),
) : StatisticsRepository {
    private val importer = StatisticsImporter(config, dao, source, transition)
    private val requests = Mutex()
    private var request: Pair<String, Deferred<Unit>>? = null
    private val status =
        MutableStateFlow<Pair<String?, ReadingSyncStatus>>(null to ReadingSyncStatus())
    private val observationRetry = ObservationRetry()
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

    override val stats = combine(data, localStatisticsClock()) { data, (today, _) ->
        data.updatedAt?.let {
            val (daily, weekly) = miniStatistics(data.days, today)
            ReadingStats(daily, weekly)
        }
    }.flowOn(Dispatchers.Default)

    init {
        scope.launch {
            tree.collectLatest { selected ->
                requests.withLock {
                    if (request?.first != selected) request?.second?.cancel()
                }
                if (selected != null) refreshAutomatically()
            }
        }
    }

    init {
        scope.launch {
            combine(tree, data, localStatisticsClock()) { selected, cached, (_, zone) ->
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

private fun statisticsIssue(error: Exception): ReadingSyncIssue = when (error) {
    is SecurityException -> ReadingSyncIssue.PermissionLost
    is MissingSourceFile -> ReadingSyncIssue.MissingData
    is UnsupportedStatisticsSchema, is SQLiteException -> ReadingSyncIssue.InvalidData
    is IOException -> ReadingSyncIssue.Provider
    else -> throw error
}

// Date/zone changes use local summaries or re-bin Room rows; the clock never reads SAF.
internal fun localStatisticsClock() = flow {
    while (true) {
        val zone = ZoneId.systemDefault()
        emit(LocalDate.now(zone) to zone)
        delay(60_000)
    }
}.distinctUntilChanged()
