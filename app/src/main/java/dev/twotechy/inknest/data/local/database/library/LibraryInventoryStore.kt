package dev.twotechy.inknest.data.local.database.library

import kotlinx.coroutines.flow.Flow

internal interface LibraryInventoryStore {
    fun observe(key: LibraryFolderKey): Flow<List<LibraryBookRow>>
    fun observeFolder(key: LibraryFolderKey): Flow<LibraryFolderEntity?>
    suspend fun books(key: LibraryFolderKey): List<LibraryBookRow>
    suspend fun putBooks(books: List<LibraryBookRow>)
    suspend fun putFolder(folder: LibraryFolderEntity)
    suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookRow>)
    suspend fun remove(key: LibraryFolderKey)
}
