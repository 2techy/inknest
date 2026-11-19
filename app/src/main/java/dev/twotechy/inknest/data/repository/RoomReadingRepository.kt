package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.CachedReading
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingCacheKey
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.source.books.FileBookCoverStore
import dev.twotechy.inknest.data.source.koreader.CurrentReadingDataSource
import dev.twotechy.inknest.data.source.koreader.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.koreader.ReadingRefreshException
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.platform.OperationTracer
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class RoomReadingRepository(
    private val config: ReadingSourceConfigRepository,
    private val cache: ReadingCache,
    private val source: CurrentReadingDataSource,
    private val scope: CoroutineScope,
    private val covers: FileBookCoverStore? = null,
    private val transition: Mutex = Mutex(),
    private val now: () -> Long = System::currentTimeMillis,
    private val permissionSnapshot: suspend () -> CoroutineContext = { EmptyCoroutineContext },
    private val tracer: OperationTracer = OperationTracer(),
) : ReadingRepository {
    private var lastRefresh: SuccessfulRefresh? = null
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
        val attempt = now()
        val token = Any()
        running.update { it + (active to token) }
        try {
            withActiveSource(active) {
                cache.attempt(active, attempt)
            }
            val result = readOrSkip(selected, active) ?: return
            saveSnapshot(active, result, attempt)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (error !is IOException && error !is SQLiteException) throw error
            recordRefreshFailure(active, error)
        } finally {
            finishRefresh(active, token)
        }
    }

    private fun finishRefresh(active: ReadingCacheKey, token: Any) {
        running.update { if (it[active] === token) it - active else it }
    }

    private suspend fun saveSnapshot(
        active: ReadingCacheKey,
        snapshot: CurrentReadingSnapshot,
        attempt: Long,
    ) = withActiveSource(active) {
        cache.save(active, snapshot, attempt)
        writeErrors.update { it - active }
        covers?.prune(cache.coverUris())
        remember(active, snapshot)
    }

    private suspend fun readOrSkip(selected: ReadingSourceConfig, active: ReadingCacheKey) =
        withContext(permissionSnapshot()) {
            if (canSkip(selected, active)) {
                tracer.begin("InkNest.reading.skip").close()
                null
            } else {
                tracer.begin("InkNest.reading.read").use { source.read(selected) }
            }
        }

    private suspend fun canSkip(selected: ReadingSourceConfig, active: ReadingCacheKey): Boolean {
        val previous = withActiveSource(active) {
            lastRefresh?.takeIf { it.key == active && healthy(it) }
        } ?: return false
        val unchanged = tracer.begin("InkNest.reading.fingerprint").use {
            source.unchanged(selected, previous.fingerprint)
        }
        if (!unchanged) return false
        return withActiveSource(active) {
            if (lastRefresh !== previous || !healthy(previous)) return@withActiveSource false
            for (uri in previous.coverUris) {
                if (covers?.exists(uri) == false) return@withActiveSource false
            }
            true
        }
    }

    private suspend fun healthy(previous: SuccessfulRefresh): Boolean {
        if (previous.key in writeErrors.value) return false
        if (readErrors.value.any { it.first == previous.key }) return false
        if (!previous.fingerprint.reusableAt(now())) return false
        return cache.current(previous.key)?.identity() == previous.cached
    }

    private suspend fun remember(active: ReadingCacheKey, snapshot: CurrentReadingSnapshot) {
        lastRefresh = null
        val fingerprint = snapshot.fingerprint ?: return
        if (snapshot.warning != null || snapshot.history == null) return
        if (snapshot.skippedHistoryEntries > 0) return
        if (snapshot.metadata.values.any { it.fingerprint == null }) return
        val cached = cache.current(active) ?: return
        if (cached.source.lastSuccessAt == null) return
        lastRefresh = SuccessfulRefresh(
            active,
            cached.identity(),
            fingerprint,
            snapshot.metadata.values.mapNotNull { it.coverUri }.toSet(),
        )
    }

    private fun CachedReading.identity() = copy(source = source.copy(lastAttemptAt = null))

    private data class SuccessfulRefresh(
        val key: ReadingCacheKey,
        val cached: CachedReading,
        val fingerprint: SourceMetadataFingerprint,
        val coverUris: Set<String>,
    )

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
            lastRefresh = null
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
        staged.snapshot?.let { cache.save(next, it, now()) }
        val previous = before.readingCacheKey() ?: return
        staged.issue?.let { cache.carryForward(previous, next, it) }
    }

    suspend fun disconnect() = transition.withLock {
        lastRefresh = null
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
            lastRefresh = null
            requests.withLock { jobs.values.forEach { it.cancel() } }
            cache.clearAll()
            covers?.clear()
        }
        refresh()
    }
}

private fun Set<Pair<ReadingCacheKey, String>>.issue(active: ReadingCacheKey?): ReadingSyncIssue? =
    if (any { it.first == active }) ReadingSyncIssue.Cache else null
