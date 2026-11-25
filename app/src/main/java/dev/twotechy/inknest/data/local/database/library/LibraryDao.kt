package dev.twotechy.inknest.data.local.database.library

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import dev.twotechy.inknest.data.local.database.books.BookEntity
import dev.twotechy.inknest.data.local.database.books.BookFileEntity
import dev.twotechy.inknest.data.local.database.books.BookProfileDao
import dev.twotechy.inknest.data.local.database.books.bookFileId
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LibraryDao : BookProfileDao {
    @Query(
        "SELECT fingerprint FROM library_book_rows WHERE sourceId=:source AND path=:path ORDER BY folderKey LIMIT 1",
    )
    fun observeFingerprint(source: String, path: String): Flow<String?>

    @Query(
        "SELECT * FROM library_book_rows WHERE sourceId=:source AND folderKey=:folder ORDER BY path",
    )
    fun observe(source: String, folder: String): Flow<List<LibraryBookRow>>

    @Query("SELECT * FROM library_folders WHERE sourceId=:source AND folderKey=:folder")
    fun observeFolder(source: String, folder: String): Flow<LibraryFolderEntity?>

    @Query("SELECT * FROM library_book_rows WHERE sourceId=:source AND folderKey=:folder")
    suspend fun books(source: String, folder: String): List<LibraryBookRow>

    @Query(
        "SELECT * FROM library_book_rows WHERE sourceId=:source AND folderKey=:folder AND path=:path",
    )
    suspend fun book(source: String, folder: String, path: String): LibraryBookRow?

    @Query(
        "SELECT checksum FROM library_book_rows WHERE sourceId=:source AND folderKey=:folder AND path=:path AND fingerprint=:fingerprint",
    )
    suspend fun checksum(source: String, folder: String, path: String, fingerprint: String): String?

    @Query(
        """
        UPDATE book_files SET checksum=:checksum WHERE fingerprint=:fingerprint AND id=(
            SELECT fileId FROM library_books WHERE sourceId=:source AND folderKey=:folder AND path=:path
        )
        """,
    )
    suspend fun saveChecksum(
        source: String,
        folder: String,
        path: String,
        fingerprint: String,
        checksum: String,
    )

    @Transaction
    suspend fun putBooks(books: List<LibraryBookRow>) {
        val files = books.map { book ->
            val id =
                bookFileId(book.sourceId, book.path, book.fingerprint, "library:${book.folderKey}")
            BookFileEntity(id, id, book.sourceId, book.path, book.fingerprint, book.checksum)
        }
        putProfiles(
            books.zip(files) { book, file ->
                BookEntity(file.bookId, book.title, book.author, book.coverUri, book.description)
            },
        )
        insertFiles(files)
        for (file in files) {
            file.checksum?.let { fillMissingChecksum(file.id, it) }
        }
        putMemberships(
            books.zip(files) { book, file ->
                LibraryBookEntity(
                    book.sourceId,
                    book.folderKey,
                    book.path,
                    file.id,
                    book.progress,
                    book.status,
                )
            },
        )
    }

    @Upsert suspend fun putMemberships(books: List<LibraryBookEntity>)

    @Query("UPDATE book_files SET checksum=:checksum WHERE id=:id AND checksum IS NULL")
    suspend fun fillMissingChecksum(id: String, checksum: String)

    @Upsert suspend fun putFolderRow(folder: LibraryFolderEntity)

    @Transaction suspend fun putFolder(folder: LibraryFolderEntity) {
        putFolderRow(folder)
        if (folder.complete) {
            pruneFiles()
            pruneProfiles("file:*")
        }
    }

    @Query("DELETE FROM library_books WHERE sourceId=:source AND folderKey=:folder")
    suspend fun deleteBooks(source: String, folder: String)

    @Query("DELETE FROM library_folders WHERE sourceId=:source AND folderKey=:folder")
    suspend fun deleteFolder(source: String, folder: String)

    @Transaction suspend fun removeFolder(source: String, folder: String) {
        deleteBooks(source, folder)
        deleteFolder(source, folder)
        pruneFiles()
        pruneProfiles("file:*")
    }

    @Transaction suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookRow>) {
        deleteBooks(folder.sourceId, folder.folderKey)
        putBooks(books)
        putFolderRow(folder)
        pruneFiles()
        pruneProfiles("file:*")
    }
}
