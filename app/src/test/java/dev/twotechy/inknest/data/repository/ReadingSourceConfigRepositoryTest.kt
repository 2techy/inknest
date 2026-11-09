package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.model.ReadingSourceConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingSourceConfigRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `independent source keys preserve pins and preferences`() = runTest {
        val file = folder.newFile("source.preferences_pb")
        file.delete()
        val store = testPreferencesDataStore(file, backgroundScope)
        val source = ReadingSourceConfigRepository(store)
        val user = DataStoreUserPreferencesRepository(store)
        user.pin("reader/Activity")
        user.setShowHomeStats(false)
        assertEquals(ReadingSourceConfig(), source.config.first())
        val initial = source.config.first()
        source.activateTree(
            initial,
            source.treeCandidate(initial, SourceFolder.KOReader, "content://tree/koreader"),
        )
        val id = source.config.first().sourceId
        assertNotNull(id)
        val reader = source.config.first()
        source.activateTree(
            reader,
            source.treeCandidate(reader, SourceFolder.Books, "content://tree/books"),
        )
        val saved = ReadingSourceConfigRepository(store).config.first()
        assertEquals(id, saved.sourceId)
        assertEquals("content://tree/koreader", saved.koreaderTreeUri)
        assertEquals(setOf("content://tree/books"), saved.booksTreeUris)
        assertEquals(
            id,
            source.treeCandidate(
                saved,
                SourceFolder.KOReader,
                checkNotNull(saved.koreaderTreeUri),
            ).sourceId,
        )
        assertNotEquals(
            id,
            source.treeCandidate(
                saved,
                SourceFolder.KOReader,
                "content://tree/another-reader",
            ).sourceId,
        )
        source.setSourcePackage("org.koreader.launcher")
        source.setSourcePackage("different.reader")
        source.disconnect()
        assertEquals(ReadingSourceConfig(), source.config.first())
        assertEquals(listOf("reader/Activity"), user.pinnedComponentIds.first())
        assertEquals(false, user.appPreferences.first().showHomeStats)
    }

    @Test fun multipleRootsKeepSourceIdentityAndStableCacheFingerprint() = runTest {
        val file = folder.newFile("roots.preferences_pb")
        file.delete()
        val store = testPreferencesDataStore(file, backgroundScope)
        val source = ReadingSourceConfigRepository(store)
        val saved = ReadingSourceConfig("source", "reader", setOf("content://tree/books"))
        source.activateTree(source.config.first(), saved)
        val id = saved.sourceId
        val added = source.treeCandidate(saved, SourceFolder.Books, "content://tree/second")
        source.activateTree(saved, added)
        assertEquals(
            setOf("content://tree/books", "content://tree/second"),
            source.config.first().booksTreeUris,
        )
        assertEquals(id, added.sourceId)
        assertNotEquals(saved.readingCacheKey(), added.readingCacheKey())
        assertEquals(
            added.readingCacheKey(),
            added.copy(booksTreeUris = added.booksTreeUris.reversed().toSet()).readingCacheKey(),
        )
        val replaced = source.treeCandidate(
            added,
            SourceFolder.Books,
            "content://tree/third",
            "content://tree/books",
        )
        source.activateTree(added, replaced)
        assertEquals(
            setOf("content://tree/second", "content://tree/third"),
            source.config.first().booksTreeUris,
        )
        source.activateTree(replaced, replaced.copy(booksTreeUris = emptySet()))
        assertEquals(emptySet<String>(), source.config.first().booksTreeUris)
        assertEquals(id, source.config.first().sourceId)
    }
}
