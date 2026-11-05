package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.StatisticsDao
import dev.twotechy.inknest.data.local.reading.StatisticsEventEntity
import dev.twotechy.inknest.data.local.reading.StatisticsImportEntity
import dev.twotechy.inknest.data.local.reading.StatisticsSourceEntity
import dev.twotechy.inknest.data.source.stats.StatisticsEventReader
import dev.twotechy.inknest.data.source.stats.StatisticsEventSource
import dev.twotechy.inknest.model.ReadingEvent
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Raw rows never leave this boundary. Staging rows are invisible until activation succeeds.
internal class StatisticsImporter(
    private val config: ReadingSourceConfigRepository,
    private val dao: StatisticsDao,
    private val source: StatisticsEventSource,
    private val transition: Mutex,
    private val currentZone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val mutex = Mutex()
    private var initialized = false

    suspend fun refresh(tree: String) = mutex.withLock {
        initialize()
        val zone = currentZone()
        stage(tree, zone) { operation, generations ->
            source.withEvents(tree) { reader ->
                val generation = newGeneration(generations)
                var position = 0
                reader.forEachBatch { events ->
                    require(events.size <= StatisticsEventReader.BATCH_SIZE)
                    dao.append(operation, events.map { it.entity(generation, position++, zone) })
                }
                StatisticsSourceEntity(tree, generation, System.currentTimeMillis(), zone.id)
            }
        }
    }

    suspend fun rebin(tree: String, zone: ZoneId) = mutex.withLock {
        initialize()
        val previous = dao.source(tree) ?: return@withLock
        if (previous.zone == zone.id) return@withLock
        stage(tree, zone, previous.generation) { operation, generations ->
            val generation = newGeneration(generations)
            var position = -1
            while (true) {
                currentCoroutineContext().ensureActive()
                val rows = dao.batch(
                    previous.generation,
                    position,
                    StatisticsEventReader.BATCH_SIZE,
                )
                if (rows.isEmpty()) break
                dao.append(
                    operation,
                    rows.map {
                        it.copy(generation = generation, day = localDay(it.startedAt, zone))
                    },
                )
                position = rows.last().position
            }
            previous.copy(generation = generation, zone = zone.id)
        }
    }

    private suspend fun initialize() {
        if (initialized) return
        transition.withLock { dao.pruneStaging() }
        initialized = true
    }

    private suspend fun stage(
        tree: String,
        zone: ZoneId,
        requiredGeneration: String? = null,
        read: suspend (StatisticsImportEntity, MutableList<String>) -> StatisticsSourceEntity,
    ) {
        val operation = StatisticsImportEntity(UUID.randomUUID().toString(), tree)
        val expected = transition.withLock {
            if (config.config.first().koreaderTreeUri != tree) return
            val active = dao.source(tree)?.generation
            if (requiredGeneration != null && requiredGeneration != active) return
            dao.begin(operation)
            active
        }
        val generations = mutableListOf<String>()
        try {
            val candidate = read(operation, generations)
            dao.summarize(candidate.generation)
            publish(operation, candidate, expected)
        } finally {
            withContext(NonCancellable) {
                // Cancellation can arrive after Room commits but before its result is delivered.
                // Read the committed pointer rather than trusting an in-memory success flag.
                val active = dao.source(tree)?.generation
                generations.filter { it != active }.forEach { dao.remove(it) }
                dao.end(operation.id)
                if (active in generations && expected != null) dao.remove(expected)
            }
        }
    }

    private suspend fun publish(
        operation: StatisticsImportEntity,
        candidate: StatisticsSourceEntity,
        expected: String?,
    ): Boolean = transition.withLock {
        currentCoroutineContext().ensureActive()
        if (config.config.first().koreaderTreeUri != candidate.source) return@withLock false
        if (currentZone().id != candidate.zone) return@withLock false
        dao.activate(operation, candidate, expected)
    }

    private fun newGeneration(generations: MutableList<String>): String =
        UUID.randomUUID().toString().also(generations::add)
}

private fun ReadingEvent.entity(generation: String, position: Int, zone: ZoneId) =
    StatisticsEventEntity(
        generation,
        position,
        startedAt,
        seconds,
        bookId,
        page,
        totalPages,
        localDay(startedAt, zone),
    )

private fun localDay(startedAt: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate().toEpochDay()
