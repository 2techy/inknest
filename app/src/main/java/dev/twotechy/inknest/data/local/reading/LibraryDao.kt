package dev.twotechy.inknest.data.local.reading

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LibraryDao {
    @Query("SELECT * FROM library_books WHERE sourceId=:source AND folderKey=:folder ORDER BY path")
    fun observe(source: String, folder: String): Flow<List<LibraryBookEntity>>

    @Query("SELECT * FROM library_folders WHERE sourceId=:source AND folderKey=:folder")
    fun observeFolder(source: String, folder: String): Flow<LibraryFolderEntity?>

    @Query("SELECT * FROM library_books WHERE sourceId=:source AND folderKey=:folder")
    suspend fun books(source: String, folder: String): List<LibraryBookEntity>

    @Query(
        "SELECT * FROM library_books WHERE sourceId=:source AND folderKey=:folder AND path=:path",
    )
    suspend fun book(source: String, folder: String, path: String): LibraryBookEntity?

    @Upsert suspend fun putBooks(books: List<LibraryBookEntity>)

    @Upsert suspend fun putFolder(folder: LibraryFolderEntity)

    @Query("DELETE FROM library_books WHERE sourceId=:source AND folderKey=:folder")
    suspend fun deleteBooks(source: String, folder: String)

    @Transaction suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookEntity>) {
        deleteBooks(folder.sourceId, folder.folderKey)
        putBooks(books)
        putFolder(folder)
    }
}
