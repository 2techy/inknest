package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.data.repository.LibraryCatalog
import dev.twotechy.inknest.data.repository.LibraryFolder
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAuthorsTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @org.junit.After
    fun resetDispatcher() = kotlinx.coroutines.Dispatchers.resetMain()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun browsingRestoresAndFolderChangeClearsAuthorWithoutRefresh() =
        kotlinx.coroutines.test.runTest {
            val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
            kotlinx.coroutines.Dispatchers.setMain(dispatcher)
            val store = androidx.lifecycle.ViewModelStore()
            try {
                val repository = object : dev.twotechy.inknest.data.repository.LibraryRepository {
                    override val catalog = kotlinx.coroutines.flow.MutableStateFlow(
                        LibraryCatalog(listOf(book("1", "Writer")), configured = true),
                    )
                    override suspend fun refresh() {
                        error("Browsing must not scan")
                    }
                }
                val saved = androidx.lifecycle.SavedStateHandle(
                    mapOf(
                        "libraryBrowse" to LibraryBrowse.Authors.name,
                        "libraryAuthor" to libraryAuthorKey("Writer"),
                        "libraryAuthorName" to "Writer",
                    ),
                )
                val vm =
                    LibraryViewModel(
                        repository,
                        saved,
                        dev.twotechy.inknest.data.repository.TestReadingOpenRepository(),
                        dispatcher,
                    )
                store.put("library", vm)
                backgroundScope.launch(
                    kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler),
                ) {
                    vm.uiState.collect()
                }
                runCurrent()
                assertEquals(1, vm.uiState.value.books.size)
                assertTrue(!vm.uiState.value.authorIndex)
                vm.backToAuthors()
                runCurrent()
                assertTrue(vm.uiState.value.authorIndex)
                vm.author(vm.uiState.value.authors.single())
                vm.folder(null)
                runCurrent()
                assertTrue(vm.uiState.value.authorIndex)
                vm.browse(LibraryBrowse.Books)
                runCurrent()
                assertEquals(LibraryBrowse.Books, vm.uiState.value.options.browse)
            } finally {
                store.clear()
            }
        }

    private fun book(id: String, author: String?, title: String = id) =
        ReadingItem(ReadingItemId("source", id), title, author, null)

    @Test fun normalizedAuthorsKeepAccentsAndUnknownSeparate() {
        val authors =
            libraryAuthors(
                listOf(
                    book("1", "  TOLKIEN  "),
                    book("2", "tolkien"),
                    book("3", null),
                    book("4", "  "),
                    book("5", "Unknown author"),
                    book("6", "A, B"),
                ),
            )
        assertEquals(4, authors.size)
        assertEquals(2, authors.single { it.key == libraryAuthorKey("Tolkien") }.count)
        assertEquals(2, authors.last().count)
        assertEquals("unknown:", authors.last().key)
        assertTrue(libraryAuthorKey("Nguy\u1ec5n") != libraryAuthorKey("Nguyen"))
        assertEquals(libraryAuthorKey("A B"), libraryAuthorKey(" A   B "))
    }

    @Test fun folderSearchAndStatusApplyBeforeAuthorCounts() {
        val first = book("1", "Writer", "Match").copy(status = ReadingStatus.Reading)
        val books = listOf(first, book("2", "Writer", "Other"), book("3", "Second", "Match"))
        val folder = LibraryFolder("tree", setOf(first.id, books[1].id), ReadingSyncStatus(), false)
        val state =
            libraryUiState(
                LibraryCatalog(books, configured = true, folders = listOf(folder)),
                LibraryOptions(
                    query = "Match",
                    filter = LibraryFilter.Reading,
                    folderUri = "tree",
                    browse = LibraryBrowse.Authors,
                ),
            )
        assertEquals(1, state.authors.size)
        assertEquals(1, state.authors.single().count)
        assertEquals(listOf(first), state.books)
    }

    @Test fun missingAuthorAfterRefreshRemainsAnEmptyShelf() {
        val options =
            LibraryOptions(
                browse = LibraryBrowse.Authors,
                authorKey = libraryAuthorKey("Writer"),
                authorName = "Writer",
            )
        val state =
            libraryUiState(
                LibraryCatalog(listOf(book("1", "Someone else")), configured = true),
                options,
            )
        assertTrue(state.books.isEmpty())
        assertEquals(options.authorKey, state.options.authorKey)
        assertTrue(!state.authorIndex)
    }
}
