package dev.twotechy.inknest.data.local.database.books

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "books")
internal data class BookEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val author: String? = null,
    val coverUri: String? = null,
    val description: String? = null,
)
