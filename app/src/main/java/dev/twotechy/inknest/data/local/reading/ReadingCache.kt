package dev.twotechy.inknest.data.local.reading

import dev.twotechy.inknest.data.source.CurrentReadingSnapshot
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.security.MessageDigest

internal data class ReadingCacheKey(val sourceId: String, val snapshotKey: String)

internal fun ReadingSourceConfig.readingCacheKey(): ReadingCacheKey? {
    val id = sourceId ?: return null
    val tree = koreaderTreeUri ?: return null
    val bytes = (tree + "\n" + booksTreeUri.orEmpty()).toByteArray(Charsets.UTF_8)
    val fingerprint = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it)
    }
    return ReadingCacheKey(id, fingerprint)
}

// Candidate tree assignments have their own partition, so staging never overwrites active cache.
internal class ReadingCache(private val dao: ReadingDao) {
    suspend fun clearAll() = dao.clearAll()
    fun observe(key: ReadingCacheKey) = dao.observe(key.sourceId, key.snapshotKey)
    suspend fun attempt(key: ReadingCacheKey, time: Long) =
        dao.attempt(key.sourceId, key.snapshotKey, time)
    suspend fun fail(key: ReadingCacheKey, issue: ReadingSyncIssue) =
        dao.updateError(key.sourceId, key.snapshotKey, issue.name)
    suspend fun clear(key: ReadingCacheKey) = dao.clear(key.sourceId, key.snapshotKey)

    suspend fun save(key: ReadingCacheKey, snapshot: CurrentReadingSnapshot, attempt: Long) {
        val item = snapshot.current
        val book = item?.let {
            ReadingBookEntity(
                key.sourceId,
                key.snapshotKey,
                it.id.path,
                it.title,
                it.availability.name,
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
            ),
            book,
        )
    }
}
