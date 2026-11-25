package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import dev.twotechy.inknest.data.local.database.books.BookEntity
import dev.twotechy.inknest.data.local.database.books.BookProfileDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Dao
internal interface StatisticsDao : BookProfileDao {
    @Upsert suspend fun putBookAliases(books: List<StatisticsBookEntity>)

    @Transaction suspend fun putBooks(books: List<StatisticsBookRow>) {
        val profiles = books.map { BookEntity("stats:${it.generation}:${it.bookId}", it.title) }
        putProfiles(profiles)
        putBookAliases(
            books.zip(profiles) { row, profile ->
                StatisticsBookEntity(row.generation, row.bookId, row.checksum, profile.id)
            },
        )
    }

    @Query(
        """
        SELECT profiles.* FROM statistics_books s JOIN books profiles ON profiles.id=COALESCE(
            (SELECT CASE WHEN COUNT(DISTINCT f.bookId)=1 THEN MIN(f.bookId) END
             FROM book_files f WHERE f.sourceId=:fileSource AND f.checksum=s.checksum),
            s.profileId
        )
        WHERE s.generation=:generation AND s.bookId=:bookId
            AND EXISTS (SELECT 1 FROM statistics_sources WHERE generation=s.generation)
        """,
    )
    fun observeBookProfile(generation: String, bookId: Long, fileSource: String?): Flow<BookEntity?>

    @Transaction
    suspend fun appendBooks(operation: StatisticsImportEntity, books: List<StatisticsBookRow>) {
        if (findOperation(operation.id) == null) {
            throw CancellationException("Reading cache cleared")
        }
        putBooks(books)
    }

    @Query(
        """
        INSERT INTO statistics_books SELECT :generation, bookId, checksum, profileId
        FROM statistics_books WHERE generation=:previous
        """,
    )
    suspend fun copyBooks(previous: String, generation: String)

    @Query(
        """
        SELECT COUNT(*) FROM statistics_books
        WHERE generation=:generation AND checksum=:checksum
        """,
    )
    fun observeMatches(generation: String, checksum: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(seconds),0) AS seconds,
            CASE WHEN MAX(CASE WHEN pages IS NULL THEN 1 ELSE 0 END)=1 THEN NULL
                ELSE COALESCE(SUM(pages),0) END AS pages,
            COUNT(*) AS days, MIN(day) AS firstDay, MAX(day) AS lastDay
        FROM ($BOOK_DAYS_SQL)
        """,
    )
    fun observeBookTotals(
        generation: String,
        checksum: String,
        today: Long,
    ): Flow<BookStatisticsTotals>

    @Query("$BOOK_DAYS_SQL ORDER BY r.day DESC LIMIT :limit OFFSET :offset")
    fun observeBookDays(
        generation: String,
        checksum: String,
        today: Long,
        limit: Int,
        offset: Int = 0,
    ): Flow<List<BookStatisticsDay>>

    @Transaction
    @Query("SELECT * FROM statistics_sources WHERE source=:source")
    fun observeSnapshot(source: String): Flow<StatisticsSnapshot?>

    @Query("SELECT * FROM statistics_sources WHERE source=:source")
    suspend fun source(source: String): StatisticsSourceEntity?

    @Query("SELECT * FROM statistics_imports WHERE id=:id")
    suspend fun findOperation(id: String): StatisticsImportEntity?

    @Upsert suspend fun begin(operation: StatisticsImportEntity)

    @Upsert suspend fun putEvents(events: List<StatisticsEventEntity>)

    @Upsert suspend fun putSource(source: StatisticsSourceEntity)

    @Transaction
    suspend fun append(operation: StatisticsImportEntity, events: List<StatisticsEventEntity>) {
        if (findOperation(operation.id) ==
            null
        ) {
            throw CancellationException("Reading cache cleared")
        }
        putEvents(events)
    }

    @Query(
        """
        INSERT INTO statistics_days (generation, day, seconds, pages)
        SELECT r.generation, r.day, SUM(r.seconds),
            CASE WHEN MAX(CASE WHEN r.page IS NULL THEN 1 ELSE 0 END)=1
                THEN NULL ELSE COALESCE(p.pages, 0) END
        FROM statistics_events r
        LEFT JOIN (
            SELECT day, COUNT(*) AS pages FROM (
                SELECT day, bookId, page, totalPages FROM statistics_events
                WHERE generation=:generation AND seconds>0 AND page IS NOT NULL
                GROUP BY day, bookId, page, totalPages
            ) GROUP BY day
        ) p ON p.day=r.day
        WHERE r.generation=:generation AND r.seconds>0
        GROUP BY r.generation, r.day
    """,
    )
    suspend fun summarize(generation: String)

    @Query(
        "SELECT * FROM statistics_events WHERE generation=:generation AND position>:after ORDER BY position LIMIT :limit",
    )
    suspend fun batch(generation: String, after: Int, limit: Int): List<StatisticsEventEntity>

    @Transaction suspend fun activate(
        operation: StatisticsImportEntity,
        candidate: StatisticsSourceEntity,
        expected: String?,
    ): Boolean {
        if (findOperation(operation.id) ==
            null
        ) {
            throw CancellationException("Reading cache cleared")
        }
        if (source(candidate.source)?.generation != expected) return false
        putSource(candidate)
        return true
    }

    @Query("DELETE FROM statistics_events WHERE generation=:generation")
    suspend fun deleteEvents(generation: String)

    @Query("DELETE FROM statistics_days WHERE generation=:generation")
    suspend fun deleteDays(generation: String)

    @Query("DELETE FROM statistics_imports WHERE id=:id")
    suspend fun end(id: String)

    @Transaction suspend fun remove(generation: String) {
        deleteBooks(generation)
        deleteDays(generation)
        deleteEvents(generation)
        pruneProfiles("stats:*")
    }

    @Query(
        "DELETE FROM statistics_days WHERE generation NOT IN (SELECT generation FROM statistics_sources)",
    )
    suspend fun pruneDays()

    @Query(
        "DELETE FROM statistics_events WHERE generation NOT IN (SELECT generation FROM statistics_sources)",
    )
    suspend fun pruneEvents()

    @Query("DELETE FROM statistics_imports")
    suspend fun clearOperations()

    @Transaction suspend fun pruneStaging() {
        pruneBooks()
        pruneDays()
        pruneEvents()
        clearOperations()
        pruneProfiles("stats:*")
    }

    @Query("DELETE FROM statistics_books WHERE generation=:generation")
    suspend fun deleteBooks(generation: String)

    @Query(
        "DELETE FROM statistics_books WHERE generation NOT IN (SELECT generation FROM statistics_sources)",
    )
    suspend fun pruneBooks()
}

private const val BOOK_DAYS_SQL = """
    SELECT r.day AS day, SUM(r.seconds) AS seconds,
        CASE WHEN MAX(CASE WHEN r.page IS NULL THEN 1 ELSE 0 END)=1 THEN NULL
        ELSE (SELECT COUNT(*) FROM (
            SELECT e.bookId, e.page, e.totalPages FROM statistics_events e
            JOIN statistics_books b ON b.generation=e.generation AND b.bookId=e.bookId
            WHERE e.generation=:generation AND b.checksum=:checksum AND e.day=r.day
                AND e.seconds>0 AND e.page IS NOT NULL
            GROUP BY e.bookId, e.page, e.totalPages
        )) END AS pages
    FROM statistics_events r
    JOIN statistics_books b ON b.generation=r.generation AND b.bookId=r.bookId
    WHERE r.generation=:generation AND b.checksum=:checksum AND r.seconds>0 AND r.day<=:today
    GROUP BY r.day
"""
