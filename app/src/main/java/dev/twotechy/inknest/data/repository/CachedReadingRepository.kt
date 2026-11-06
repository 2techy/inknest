package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingCacheKey
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.source.BookCoverCache
import dev.twotechy.inknest.data.source.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.CurrentReadingSource
import dev.twotechy.inknest.data.source.ReadingRefreshException
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
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
    private val covers: BookCoverCache? = null,
    private val transition: Mutex = Mutex(),
) : ReadingRepository {
    private val requests = Mutex()
    private val jobs = mutableMapOf<ReadingCacheKey, Deferred<Unit>>()
    private val running = MutableStateFlow<Map<ReadingCacheKey, Any>>(emptyMap())
    private val writeErrors = MutableStateFlow<Map<ReadingCacheKey, ReadingSyncIssue>>(emptyMap())
    private val observationRetry = ObservationRetry()
    private val readErrors = MutableStateFlow<Set<Pair<ReadingCacheKey, String>>>(emptySet())
    private val key = config.config.map { it.readingCacheKey() }.distinctUntilChanged()
    private val observation = key.flatMapLatest { active ->
        if (active ==
            null
        ) {
            flowOf(ReadingCacheObservation(null))
        } else {
            cache.observe(active).map {
                ReadingCacheObservation(active, it)
            }.recoverObservation(
                observationRetry,
                onError = { readErrors.update { it + (active to "current") } },
                onSuccess = { readErrors.update { it - (active to "current") } },
            ).onStart { emit(ReadingCacheObservation(active)) }
        }
    }.stateIn(scope, SharingStarted.Eagerly, ReadingCacheObservation(null))

    private val history = key.flatMapLatest { active ->
        if (active ==
            null
        ) {
            flowOf(null to emptyList())
        } else {
            cache.observeHistory(active).map { rows ->
                active to rows.map { it.toReadingItem() }
            }.recoverObservation(
                observationRetry,
                onError = { readErrors.update { it + (active to "history") } },
                onSuccess = { readErrors.update { it - (active to "history") } },
            ).onStart { emit(active to emptyList()) }
        }
    }
    override val trackedItems = combine(history, key) { (observed, items), active ->
        if (observed == active) items else emptyList()
    }

    override val reading =
        combine(observation, key, writeErrors, readErrors) { state, active, errors, reads ->
            state.readingState(active, errors[active] ?: reads.issue(active))
        }
    override val sync =
        combine(observation, key, running, writeErrors, readErrors) {
                state,
                active,
                inFlight,
                errors,
                reads,
            ->
            state.syncStatus(
                active,
                active != null && active in inFlight,
                errors[active] ?: reads.issue(active),
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
        observationRetry.retry()
        config.retryRead()
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
            withActiveSource(active) {
                cache.attempt(active, attempt)
            }
            val result = source.read(selected)
            withActiveSource(active) {
                cache.save(active, result, attempt)
                writeErrors.update { it - active }
                covers?.prune(cache.coverUris())
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
        withActiveSource(active) {
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

    private suspend fun <T> withActiveSource(active: ReadingCacheKey, update: suspend () -> T): T =
        transition.withLock {
            ensureActiveSource(active)
            update()
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
        val staged = stageCandidate(before, candidate)
        transition.withLock {
            if (config.config.first() != before) throw IOException("Source selection changed")
            saveCandidate(before, staged)
            config.activateTree(before, candidate)
            staged.key?.let { next -> writeErrors.update { it - next } }
            covers?.prune(cache.coverUris())
        }
    }

    private data class StagedReading(
        val key: ReadingCacheKey?,
        val snapshot: CurrentReadingSnapshot? = null,
        val issue: ReadingSyncIssue? = null,
    )

    private suspend fun stageCandidate(
        before: ReadingSourceConfig,
        candidate: ReadingSourceConfig,
    ): StagedReading {
        val key = candidate.readingCacheKey() ?: return StagedReading(null)
        return try {
            StagedReading(key, source.read(candidate))
        } catch (failure: ReadingRefreshException) {
            if (before.koreaderTreeUri != candidate.koreaderTreeUri) throw failure
            StagedReading(key, issue = failure.issue)
        }
    }

    private suspend fun saveCandidate(before: ReadingSourceConfig, staged: StagedReading) {
        val next = staged.key ?: return
        staged.snapshot?.let { cache.save(next, it, System.currentTimeMillis()) }
        val previous = before.readingCacheKey() ?: return
        staged.issue?.let { cache.carryForward(previous, next, it) }
    }

    suspend fun disconnect() = transition.withLock {
        requests.withLock { jobs.values.forEach { it.cancel() } }
        config.disconnect()
        writeErrors.value = emptyMap()
        cache.clearAll()
        covers?.clear()
    }

    suspend fun clearCache() {
        transition.withLock {
            val selected = config.config.first()
            if (selected.koreaderTreeUri == null && selected.booksTreeUris.isEmpty()) return
            requests.withLock { jobs.values.forEach { it.cancel() } }
            cache.clearAll()
            covers?.clear()
        }
        refresh()
    }
}

private fun Set<Pair<ReadingCacheKey, String>>.issue(active: ReadingCacheKey?): ReadingSyncIssue? =
    if (any { it.first == active }) ReadingSyncIssue.Cache else null
