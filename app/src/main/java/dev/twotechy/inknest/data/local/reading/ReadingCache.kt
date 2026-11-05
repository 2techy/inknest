package dev.twotechy.inknest.data.local.reading

import dev.twotechy.inknest.data.source.BookMetadata
import dev.twotechy.inknest.data.source.CurrentReadingSnapshot
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ReadingCacheKey(val sourceId: String, val snapshotKey: String)

internal fun ReadingSourceConfig.readingCacheKey(): ReadingCacheKey? {
    val id = sourceId ?: return null
    val tree = koreaderTreeUri ?: return null
    val bytes = (
        tree + "\n" + booksTreeUris.sorted().joinToString(
            "\n",
        )
        ).toByteArray(Charsets.UTF_8)
    val fingerprint = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it)
    }
    return ReadingCacheKey(id, fingerprint)
}

// Candidate tree assignments have their own partition, so staging never overwrites active cache.
internal class ReadingCache(private val dao: ReadingDao) {
    suspend fun clearAll() = dao.clearAll()
    suspend fun carryForward(
        previous: ReadingCacheKey,
        next: ReadingCacheKey,
        issue: ReadingSyncIssue,
    ) = dao.carryForward(previous, next, issue.name)
    fun observe(key: ReadingCacheKey) = dao.observe(key.sourceId, key.snapshotKey)
    suspend fun attempt(key: ReadingCacheKey, time: Long) =
        dao.attempt(key.sourceId, key.snapshotKey, time)
    suspend fun fail(key: ReadingCacheKey, issue: ReadingSyncIssue) =
        dao.updateError(key.sourceId, key.snapshotKey, issue.name)
    suspend fun clear(key: ReadingCacheKey) = dao.clear(key.sourceId, key.snapshotKey)

    fun observeHistory(key: ReadingCacheKey) = dao.observeHistory(key.sourceId, key.snapshotKey)
    suspend fun coverUris() = dao.coverUris().toSet()
    suspend fun metadata(config: ReadingSourceConfig, path: String): BookMetadata? {
        val key = config.readingCacheKey() ?: return null
        val saved =
            dao.book(key.sourceId, key.snapshotKey, path) ?: dao.recentBook(key.sourceId, path)
        return saved?.let {
            BookMetadata(
                it.fingerprint,
                it.title,
                it.author,
                it.coverUri,
            )
        }
    }

    suspend fun save(key: ReadingCacheKey, snapshot: CurrentReadingSnapshot, attempt: Long) =
        withContext(Dispatchers.Default) {
            val item = snapshot.current
            val history = snapshot.history.orEmpty().distinctBy { it.id.path }
            val order = history.mapIndexed { index, book -> book.id.path to index }.toMap()
            val books = (history + listOfNotNull(item)).associateBy { it.id.path }.values.map {
                ReadingBookEntity(
                    sourceId = key.sourceId,
                    snapshotKey = key.snapshotKey,
                    path = it.id.path,
                    title = it.title,
                    availability = it.availability.name,
                    author = it.author,
                    progress = it.progress,
                    coverUri = it.coverUri,
                    status = it.status.name,
                    lastReadAt = it.lastReadAt,
                    historyOrder = order[it.id.path],
                    fingerprint = snapshot.metadata[it.id.path]?.fingerprint,
                )
            }
            dao.save(
                ReadingSourceEntity(
                    key.sourceId,
                    key.snapshotKey,
                    currentPath = item?.id?.path,
                    lastAttemptAt = attempt,
                    lastSuccessAt = System.currentTimeMillis(),
                    error = snapshot.warning?.name,
                    skippedHistoryEntries = snapshot.skippedHistoryEntries,
                ),
                books,
                replaceHistory = snapshot.history != null,
            )
        }
}
