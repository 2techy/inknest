package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import dev.twotechy.inknest.data.local.database.books.BookEntity

@Entity(
    tableName = "statistics_books",
    primaryKeys = ["generation", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
        ),
    ],
    indices = [Index(value = ["generation", "checksum"]), Index("profileId")],
)
internal data class StatisticsBookEntity(
    val generation: String,
    val bookId: Long,
    val checksum: String?,
    val profileId: String,
)
