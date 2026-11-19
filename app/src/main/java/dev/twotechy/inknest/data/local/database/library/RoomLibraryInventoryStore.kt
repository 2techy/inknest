package dev.twotechy.inknest.data.local.database.library

internal class RoomLibraryInventoryStore(private val dao: LibraryDao) : LibraryInventoryStore {
    override fun observe(key: LibraryFolderKey) = dao.observe(key.source, key.tree)
    override fun observeFolder(key: LibraryFolderKey) = dao.observeFolder(key.source, key.tree)
    override suspend fun books(key: LibraryFolderKey) = dao.books(key.source, key.tree)
    override suspend fun putBooks(books: List<LibraryBookEntity>) = dao.putBooks(books)
    override suspend fun putFolder(folder: LibraryFolderEntity) = dao.putFolder(folder)
    override suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookEntity>) =
        dao.replace(folder, books)
    override suspend fun remove(key: LibraryFolderKey) = dao.removeFolder(key.source, key.tree)
}
