package dev.twotechy.inknest.data.local.reading

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

private const val CURRENT_QUERY = """
    SELECT s.*, b.sourceId AS book_sourceId, b.snapshotKey AS book_snapshotKey,
        b.path AS book_path, b.title AS book_title, b.availability AS book_availability,
        b.author AS book_author, b.progress AS book_progress, b.coverUri AS book_coverUri,
        b.status AS book_status, b.lastReadAt AS book_lastReadAt, b.historyOrder AS book_historyOrder,
        b.fingerprint AS book_fingerprint
    FROM reading_sources s LEFT JOIN reading_items b
        ON s.sourceId=b.sourceId AND s.snapshotKey=b.snapshotKey AND s.currentPath=b.path
    WHERE s.sourceId=:sourceId AND s.snapshotKey=:snapshotKey
"""

@Dao
internal interface ReadingDao {
    @Query(CURRENT_QUERY)
    fun observe(sourceId: String, snapshotKey: String): Flow<CachedReading?>

    @Query(CURRENT_QUERY)
    suspend fun get(sourceId: String, snapshotKey: String): CachedReading?

    @Query(
        "SELECT * FROM reading_items WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey AND historyOrder IS NOT NULL ORDER BY historyOrder",
    )
    fun observeHistory(sourceId: String, snapshotKey: String): Flow<List<ReadingBookEntity>>

    @Query(
        "SELECT * FROM reading_items WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey AND path=:path",
    )
    suspend fun book(sourceId: String, snapshotKey: String, path: String): ReadingBookEntity?

    @Query(
        """
        SELECT b.* FROM reading_items b JOIN reading_sources s
        ON b.sourceId=s.sourceId AND b.snapshotKey=s.snapshotKey
        WHERE b.sourceId=:sourceId AND b.path=:path
        ORDER BY s.lastSuccessAt DESC LIMIT 1
    """,
    )
    suspend fun recentBook(sourceId: String, path: String): ReadingBookEntity?

    @Query("SELECT * FROM reading_items WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey")
    suspend fun books(sourceId: String, snapshotKey: String): List<ReadingBookEntity>

    @Transaction
    suspend fun carryForward(previous: ReadingCacheKey, next: ReadingCacheKey, issue: String) {
        val old = get(previous.sourceId, previous.snapshotKey) ?: run {
            attempt(next.sourceId, next.snapshotKey, System.currentTimeMillis())
            updateError(next.sourceId, next.snapshotKey, issue)
            return
        }
        val rows = books(previous.sourceId, previous.snapshotKey).map {
            it.copy(
                sourceId = next.sourceId,
                snapshotKey = next.snapshotKey,
                availability = "Unresolved",
            )
        }
        save(
            old.source.copy(
                sourceId = next.sourceId,
                snapshotKey = next.snapshotKey,
                error = issue,
            ),
            rows,
            replaceHistory = true,
        )
    }

    @Query(
        "SELECT coverUri FROM reading_items WHERE coverUri IS NOT NULL UNION SELECT coverUri FROM library_books WHERE coverUri IS NOT NULL",
    )
    suspend fun coverUris(): List<String>

    @Query(
        "UPDATE reading_items SET historyOrder=NULL WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey",
    )
    suspend fun resetHistory(sourceId: String, snapshotKey: String)

    @Query(
        "DELETE FROM reading_items WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey AND historyOrder IS NULL AND (:currentPath IS NULL OR path != :currentPath)",
    )
    suspend fun pruneBooks(sourceId: String, snapshotKey: String, currentPath: String?)

    @Upsert suspend fun putBook(book: ReadingBookEntity)

    @Upsert suspend fun putSource(source: ReadingSourceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureSource(source: ReadingSourceEntity)

    @Query(
        "UPDATE reading_sources SET lastAttemptAt=:time WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey",
    )
    suspend fun updateAttempt(sourceId: String, snapshotKey: String, time: Long)

    @Query(
        "UPDATE reading_sources SET error=:error WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey",
    )
    suspend fun updateError(sourceId: String, snapshotKey: String, error: String)

    @Transaction
    suspend fun attempt(sourceId: String, snapshotKey: String, time: Long) {
        ensureSource(ReadingSourceEntity(sourceId, snapshotKey))
        updateAttempt(sourceId, snapshotKey, time)
    }

    @Transaction
    suspend fun save(
        source: ReadingSourceEntity,
        books: List<ReadingBookEntity>,
        replaceHistory: Boolean,
    ) {
        if (replaceHistory) resetHistory(source.sourceId, source.snapshotKey)
        for (book in books) {
            putBook(
                if (!replaceHistory && book.historyOrder == null) {
                    val previous = book(source.sourceId, source.snapshotKey, book.path)
                    book.copy(
                        historyOrder = previous?.historyOrder,
                        lastReadAt = previous?.lastReadAt,
                    )
                } else {
                    book
                },
            )
        }
        pruneBooks(source.sourceId, source.snapshotKey, source.currentPath)
        putSource(source)
    }

    @Query("DELETE FROM reading_items WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey")
    suspend fun deleteBooks(sourceId: String, snapshotKey: String)

    @Query("DELETE FROM reading_sources WHERE sourceId=:sourceId AND snapshotKey=:snapshotKey")
    suspend fun deleteSource(sourceId: String, snapshotKey: String)

    @Transaction
    suspend fun clear(sourceId: String, snapshotKey: String) {
        deleteBooks(sourceId, snapshotKey)
        deleteSource(sourceId, snapshotKey)
    }

    @Query("DELETE FROM reading_items")
    suspend fun clearBooks()

    @Query("DELETE FROM reading_sources")
    suspend fun clearSources()

    @Query("DELETE FROM library_books")
    suspend fun clearLibraryBooks()

    @Query("DELETE FROM library_folders")
    suspend fun clearLibraryFolders()

    @Transaction suspend fun clearAll() {
        clearStatisticsImports()
        clearStatisticsDays()
        clearStatisticsEvents()
        clearStatisticsSources()
        clearLibraryBooks()
        clearLibraryFolders()
        clearBooks()
        clearSources()
    }

    @Query("DELETE FROM statistics_imports")
    suspend fun clearStatisticsImports()

    @Query("DELETE FROM statistics_days")
    suspend fun clearStatisticsDays()

    @Query("DELETE FROM statistics_events")
    suspend fun clearStatisticsEvents()

    @Query("DELETE FROM statistics_sources")
    suspend fun clearStatisticsSources()
}
