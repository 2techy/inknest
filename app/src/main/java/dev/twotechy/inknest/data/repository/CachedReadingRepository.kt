package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.CachedReading
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingCacheKey
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.source.CurrentReadingSource
import dev.twotechy.inknest.data.source.ReadingRefreshException
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class CachedReadingRepository(
    private val config: ReadingSourceConfigRepository,
    private val cache: ReadingCache,
    private val source: CurrentReadingSource,
    private val scope: CoroutineScope,
) : ReadingRepository {
    private data class Observation(val key: ReadingCacheKey?, val cached: CachedReading? = null)
    private val transition = Mutex()
    private val requests = Mutex()
    private val jobs = mutableMapOf<ReadingCacheKey, Deferred<Unit>>()
    private val running = MutableStateFlow<Map<ReadingCacheKey, Any>>(emptyMap())
    private val writeErrors = MutableStateFlow<Map<ReadingCacheKey, ReadingSyncIssue>>(emptyMap())
    private val key = config.config.map { it.readingCacheKey() }.distinctUntilChanged()
    private val observation = key.flatMapLatest { active ->
        if (active ==
            null
        ) {
            flowOf(Observation(null))
        } else {
            cache.observe(active).map {
                Observation(active, it)
            }.onStart { emit(Observation(active)) }
        }
    }.stateIn(scope, SharingStarted.Eagerly, Observation(null))

    private fun Observation.toReadingState(): ReadingState {
        val row = cached ?: return ReadingState.Loading
        val book = row.book
        if (book != null) {
            return ReadingState.Content(
                ReadingItem(
                    ReadingItemId(book.sourceId, book.path),
                    book.title,
                    null,
                    null,
                    availability = BookAvailability.valueOf(book.availability),
                ),
            )
        }
        if (row.source.lastSuccessAt != null) return ReadingState.Empty
        val issue = row.source.error ?: return ReadingState.Loading
        return ReadingState.Error(ReadingSyncIssue.valueOf(issue))
    }

    override val reading = combine(observation, key, writeErrors) { state, active, errors ->
        val failure = errors[active]
        when {
            active == null -> ReadingState.NotConfigured
            state.key != active -> ReadingState.Loading
            state.cached == null && failure != null -> ReadingState.Error(failure)
            else -> state.toReadingState()
        }
    }
    override val sync =
        combine(observation, key, running, writeErrors) { state, active, inFlight, errors ->
            val saved = state.cached?.source?.takeIf { state.key == active }
            val issue = errors[active] ?: saved?.error?.let { value ->
                ReadingSyncIssue.entries.find { it.name == value }
            }
            ReadingSyncStatus(
                saved?.lastAttemptAt,
                saved?.lastSuccessAt,
                active != null && active in inFlight,
                issue,
                saved?.lastSuccessAt != null &&
                    (issue != null || (saved.lastAttemptAt ?: 0) > saved.lastSuccessAt),
            )
        }

    init {
        scope.launch {
            key.collect { active ->
                requests.withLock {
                    if (config.config.first().readingCacheKey() != active) return@withLock
                    jobs.entries.removeAll { (key, job) ->
                        if (key != active) {
                            job.cancel()
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }
    }

    override suspend fun refresh() {
        val selected = config.config.first()
        val active = selected.readingCacheKey() ?: return
        val job = requests.withLock {
            jobs[active]?.takeIf { it.isActive } ?: scope.async(start = CoroutineStart.LAZY) {
                refreshSource(selected, active)
            }.also {
                jobs[active] = it
                it.start()
            }
        }
        // The application owns the job. Cancelling one caller only cancels its await.
        job.await()
    }

    private suspend fun refreshSource(selected: ReadingSourceConfig, active: ReadingCacheKey) {
        val attempt = System.currentTimeMillis()
        val token = Any()
        running.update { it + (active to token) }
        try {
            transition.withLock {
                ensureActiveSource(active)
                cache.attempt(active, attempt)
            }
            val result = source.read(selected)
            transition.withLock {
                ensureActiveSource(active)
                cache.save(active, result, attempt)
                writeErrors.update { it - active }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (error !is IOException && error !is SQLiteException) throw error
            recordRefreshFailure(active, error)
        } finally {
            running.update { if (it[active] === token) it - active else it }
        }
    }

    private suspend fun recordRefreshFailure(active: ReadingCacheKey, error: Exception): Nothing {
        val issue = (error as? ReadingRefreshException)?.issue ?: ReadingSyncIssue.Cache
        transition.withLock {
            ensureActiveSource(active)
            writeErrors.update { it + (active to issue) }
            try {
                cache.fail(active, issue)
            } catch (_: SQLiteException) {
                // Keep the last observed cache and expose the failure even if error persistence fails.
            } catch (_: IOException) {
                // The next successful refresh clears the transient write error.
            }
        }
        throw ReadingRefreshException(issue, error)
    }

    private suspend fun ensureActiveSource(active: ReadingCacheKey) {
        currentCoroutineContext().ensureActive()
        if (config.config.first().readingCacheKey() !=
            active
        ) {
            throw CancellationException("Reading source changed")
        }
    }

    suspend fun replaceSource(before: ReadingSourceConfig, candidate: ReadingSourceConfig) {
        if (candidate == before) {
            refresh()
            return
        }
        val nextKey = candidate.readingCacheKey()
        val snapshot = if (nextKey != null) source.read(candidate) else null
        transition.withLock {
            if (config.config.first() != before) throw IOException("Source selection changed")
            if (snapshot !=
                null
            ) {
                cache.save(checkNotNull(nextKey), snapshot, System.currentTimeMillis())
            }
            config.activateTree(before, candidate)
            if (nextKey != null) writeErrors.update { it - nextKey }
        }
    }

    suspend fun disconnect() = transition.withLock {
        requests.withLock { jobs.values.forEach { it.cancel() } }
        config.disconnect()
        writeErrors.value = emptyMap()
        cache.clearAll()
    }

    suspend fun clearCache() {
        transition.withLock {
            val active = config.config.first().readingCacheKey() ?: return
            requests.withLock { jobs[active]?.cancel() }
            cache.clearAll()
        }
        refresh()
    }
}
