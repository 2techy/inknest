package dev.twotechy.inknest.data.local.reading

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "statistics_events", primaryKeys = ["source", "position"])
internal data class StatisticsEventEntity(
    val source: String,
    val position: Int,
    val startedAt: Long,
    val seconds: Long,
    val bookId: Long,
    val page: Long?,
    val totalPages: Long?,
)

@Entity(tableName = "statistics_sources", primaryKeys = ["source"])
internal data class StatisticsSourceEntity(val source: String, val updatedAt: Long)

internal data class StatisticsSnapshot(
    @Embedded val source: StatisticsSourceEntity,
    @Relation(parentColumns = ["source"], entityColumns = ["source"])
    val events: List<StatisticsEventEntity>,
)

@Dao
internal interface StatisticsDao {
    @Transaction
    @Query("SELECT * FROM statistics_sources WHERE source=:source")
    fun observeSnapshot(source: String): Flow<StatisticsSnapshot?>

    @Query("DELETE FROM statistics_events WHERE source=:source")
    suspend fun deleteEvents(source: String)

    @Upsert suspend fun putEvents(events: List<StatisticsEventEntity>)

    @Upsert suspend fun putSource(source: StatisticsSourceEntity)

    @Transaction
    suspend fun replace(source: StatisticsSourceEntity, events: List<StatisticsEventEntity>) {
        deleteEvents(source.source)
        putEvents(events)
        putSource(source)
    }
}
