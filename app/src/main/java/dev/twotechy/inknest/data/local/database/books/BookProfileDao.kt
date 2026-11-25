package dev.twotechy.inknest.data.local.database.books

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
internal interface BookProfileDao {
    @Upsert suspend fun putProfiles(profiles: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFiles(files: List<BookFileEntity>)

    @Query(
        """
        DELETE FROM book_files WHERE
            NOT EXISTS (SELECT 1 FROM reading_items WHERE fileId=book_files.id)
            AND NOT EXISTS (SELECT 1 FROM library_books WHERE fileId=book_files.id)
        """,
    )
    suspend fun pruneFiles()

    @Query(
        """
        DELETE FROM books WHERE id GLOB :scope AND
            NOT EXISTS (SELECT 1 FROM book_files WHERE bookId=books.id)
            AND NOT EXISTS (SELECT 1 FROM statistics_books WHERE profileId=books.id)
        """,
    )
    suspend fun pruneProfiles(scope: String)
}
