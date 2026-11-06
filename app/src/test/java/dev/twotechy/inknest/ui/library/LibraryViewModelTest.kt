package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.LibraryFolder
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.TestReadingOpenRepository
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Test fun largeCatalogKeepsUnknownsLastSourceTiesAndExactBookIdentity() {
        val books = List(5000, ::book)
        val recent = selectLibraryBooks(books, LibraryOptions())
        val knownDates = recent.mapNotNull { it.lastReadAt }
        assertEquals(knownDates.sortedDescending(), knownDates)
        assertTrue(recent.drop(knownDates.size).all { it.lastReadAt == null })
        for (group in recent.groupBy { it.lastReadAt }.values) {
            assertEquals(group.sortedBy { it.id.path.toInt() }, group)
        }
        val unicode = selectLibraryBooks(books, LibraryOptions(query = "đạo kiếm"))
        assertEquals(
            books.filterIndexed { index, _ -> index % 17 == 0 }.map { it.id }.toSet(),
            unicode.map { it.id }.toSet(),
        )
        assertTrue(unicode.map { it.id }.distinct().size > 1)
        val author = selectLibraryBooks(books, LibraryOptions(query = "AUTHOR 2"))
        assertTrue(author.isNotEmpty())
        assertTrue(author.all { it.author?.contains("Author 2") == true })
        val progress = selectLibraryBooks(books, LibraryOptions(sort = LibrarySort.Progress))
        val knownProgress = progress.mapNotNull { it.progress }
        assertEquals(knownProgress.sortedDescending(), knownProgress)
        assertTrue(progress.drop(knownProgress.size).all { it.progress == null })
        for (filter in LibraryFilter.entries.drop(1)) {
            val expected = when (filter) {
                LibraryFilter.Reading -> ReadingStatus.Reading
                LibraryFilter.Finished -> ReadingStatus.Finished
                else -> ReadingStatus.Unknown
            }
            val filtered = selectLibraryBooks(books, LibraryOptions(filter = filter))
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { it.status == expected })
        }
        val titled = selectLibraryBooks(books, LibraryOptions(sort = LibrarySort.Title))
        val titles = titled.map { it.title.lowercase(java.util.Locale.ROOT) }
        assertEquals(titles.sorted(), titles)
        assertEquals(books.map { it.id }.toSet(), titled.map { it.id }.toSet())
        assertEquals(
            books.count { it.availability == BookAvailability.Missing },
            titled.count { it.availability == BookAvailability.Missing },
        )
        val fullButUnknown = book(1).copy(progress = 100, status = ReadingStatus.Unknown)
        assertTrue(
            selectLibraryBooks(
                listOf(fullButUnknown),
                LibraryOptions(filter = LibraryFilter.Finished),
            ).isEmpty(),
        )
    }

    @Test fun savedChoicesRestoreWithCachedContentDuringSourceFailureWithoutRefresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val source = CachedBooks()
            val handle = SavedStateHandle()
            val first = LibraryViewModel(source, handle, TestReadingOpenRepository(), dispatcher)
            store.put("first", first)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                first.uiState.collect()
            }
            runCurrent()
            first.folder("A")
            first.searching(true)
            first.query("needle")
            first.filter(LibraryFilter.Reading)
            first.sort(LibrarySort.Progress)
            first.toggleView()
            runCurrent()
            assertEquals(1, first.uiState.value.books.size)
            val values = handle.keys().associateWith { handle.get<Any?>(it) }
            val restored =
                LibraryViewModel(
                    source,
                    SavedStateHandle(values),
                    TestReadingOpenRepository(),
                    dispatcher,
                )
            store.put("restored", restored)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                restored.uiState.collect()
            }
            runCurrent()
            assertEquals(first.uiState.value.options, restored.uiState.value.options)
            assertEquals(LibraryViewMode.List, restored.uiState.value.options.viewMode)
            assertEquals(ReadingSyncIssue.PermissionLost, restored.uiState.value.error)
            assertEquals(first.uiState.value.books, restored.uiState.value.books)
            restored.searching(false)
            restored.query("late IME commit")
            runCurrent()
            assertEquals("", restored.uiState.value.options.query)
            assertEquals(1, restored.uiState.value.books.size)
            assertEquals("A", restored.uiState.value.options.folderUri)
            source.catalog.value = source.catalog.value.copy(
                books = source.catalog.value.books.filter { it.id.path == "B" },
                folders = source.catalog.value.folders.filter { it.uri == "B" },
            )
            runCurrent()
            assertEquals(null, restored.uiState.value.options.folderUri)
            assertEquals(listOf("B"), restored.uiState.value.books.map { it.id.path })
            assertEquals(0, source.refreshes)
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private class CachedBooks : dev.twotechy.inknest.data.repository.LibraryRepository {
        override val catalog = MutableStateFlow(
            dev.twotechy.inknest.data.repository.LibraryCatalog(
                books = listOf("A", "B").map { path ->
                    ReadingItem(
                        ReadingItemId("source", path),
                        "Needle",
                        null,
                        30,
                        status = ReadingStatus.Reading,
                    )
                },
                folders = listOf("A", "B").map { uri ->
                    LibraryFolder(
                        uri,
                        setOf(ReadingItemId("source", uri)),
                        ReadingSyncStatus(error = ReadingSyncIssue.PermissionLost, stale = true),
                        false,
                    )
                },
                configured = true,
                sync = ReadingSyncStatus(error = ReadingSyncIssue.PermissionLost, stale = true),
            ),
        )
        var refreshes = 0
        override suspend fun refresh() {
            refreshes++
        }
    }

    private fun book(index: Int) = ReadingItem(
        id = ReadingItemId("source", index.toString()),
        title = if (index % 17 == 0) {
            "ĐẠO KIẾM"
        } else {
            "Book " + index.toString().padStart(5, '0') +
                if (index % 31 == 0) " a long title".repeat(30) else ""
        },
        author = if (index % 11 == 0) null else "Author ${index % 5}",
        progress = if (index % 13 == 0) null else index % 101,
        status = ReadingStatus.entries[index % 4],
        lastReadAt = if (index % 19 == 0) null else index / 2 * 1000L,
        availability = if (index % 23 ==
            0
        ) {
            BookAvailability.Missing
        } else {
            BookAvailability.Available
        },
    )
}
