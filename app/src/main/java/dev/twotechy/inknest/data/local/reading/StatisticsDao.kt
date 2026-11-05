package dev.twotechy.inknest.data.local.reading

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Dao
internal interface StatisticsDao {
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
        deleteDays(generation)
        deleteEvents(generation)
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
        pruneDays()
        pruneEvents()
        clearOperations()
    }
}
