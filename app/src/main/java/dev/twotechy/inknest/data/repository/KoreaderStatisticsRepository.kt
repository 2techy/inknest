package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.StatisticsDao
import dev.twotechy.inknest.data.local.reading.StatisticsEventEntity
import dev.twotechy.inknest.data.local.reading.StatisticsSourceEntity
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.ReadingStats
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class KoreaderStatisticsRepository(
    private val config: ReadingSourceConfigRepository,
    private val dao: StatisticsDao,
    private val read: suspend (String) -> List<ReadingEvent>,
    private val scope: CoroutineScope,
    private val transition: Mutex = Mutex(),
) : StatisticsRepository {
    private val requests = Mutex()
    private var request: Pair<String, Deferred<Unit>>? = null
    private val status =
        MutableStateFlow<Pair<String?, ReadingSyncStatus>>(null to ReadingSyncStatus())
    private val tree = config.config.map { it.koreaderTreeUri }.distinctUntilChanged()
    override val data = tree.flatMapLatest { selected ->
        if (selected == null) {
            flowOf(StatisticsData())
        } else {
            combine(
                dao.observeSnapshot(selected).map { snapshot ->
                    StatisticsData(
                        snapshot?.events.orEmpty().map {
                            ReadingEvent(
                                it.startedAt,
                                it.seconds,
                                it.bookId,
                                it.page,
                                it.totalPages,
                            )
                        },
                        configured = true,
                        updatedAt = snapshot?.source?.updatedAt,
                    )
                },
                status,
            ) { cached, (key, sync) ->
                cached.copy(
                    sync = if (key == selected) sync else ReadingSyncStatus(),
                )
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, StatisticsData())

    override val stats = combine(data, localStatisticsClock()) { data, (today, zone) ->
        data.updatedAt?.let {
            val (daily, weekly) = miniStatistics(data.events, today, zone)
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

    private suspend fun refreshAutomatically() {
        try {
            refresh()
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
        }
    }

    override suspend fun refresh() {
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
            val events = read(tree)
            save(tree, events)
            status.value = tree to ReadingSyncStatus(lastAttemptAt = attempt)
        } catch (cancelled: CancellationException) {
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

    private suspend fun save(tree: String, events: List<ReadingEvent>) = transition.withLock {
        currentCoroutineContext().ensureActive()
        if (config.config.first().koreaderTreeUri != tree) return@withLock
        dao.replace(
            StatisticsSourceEntity(tree, System.currentTimeMillis()),
            events.mapIndexed { index, event ->
                StatisticsEventEntity(
                    tree,
                    index,
                    event.startedAt,
                    event.seconds,
                    event.bookId,
                    event.page,
                    event.totalPages,
                )
            },
        )
    }
}

private fun statisticsIssue(error: Exception): ReadingSyncIssue = when (error) {
    is SecurityException -> ReadingSyncIssue.PermissionLost
    is MissingSourceFile -> ReadingSyncIssue.MissingData
    is UnsupportedStatisticsSchema, is SQLiteException -> ReadingSyncIssue.InvalidData
    is IOException -> ReadingSyncIssue.Provider
    else -> throw error
}

// Collection stops with the visible screen. A clock tick only re-bins cached events, never reads SAF.
internal fun localStatisticsClock() = flow {
    while (true) {
        val zone = ZoneId.systemDefault()
        emit(LocalDate.now(zone) to zone)
        delay(60_000)
    }
}.distinctUntilChanged()
