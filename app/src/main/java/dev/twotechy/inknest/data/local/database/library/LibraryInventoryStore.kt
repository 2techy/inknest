package dev.twotechy.inknest.data.local.database.library

import kotlinx.coroutines.flow.Flow

internal interface LibraryInventoryStore {
    fun observe(key: LibraryFolderKey): Flow<List<LibraryBookEntity>>
    fun observeFolder(key: LibraryFolderKey): Flow<LibraryFolderEntity?>
    suspend fun books(key: LibraryFolderKey): List<LibraryBookEntity>
    suspend fun putBooks(books: List<LibraryBookEntity>)
    suspend fun putFolder(folder: LibraryFolderEntity)
    suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookEntity>)
    suspend fun remove(key: LibraryFolderKey)
}
