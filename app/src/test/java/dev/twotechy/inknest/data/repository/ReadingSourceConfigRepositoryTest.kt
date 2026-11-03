package dev.twotechy.inknest.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.twotechy.inknest.model.ReadingSourceConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingSourceConfigRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `independent source keys preserve pins and preferences`() = runTest {
        val file = folder.newFile("source.preferences_pb")
        file.delete()
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = {
            file
        })
        val source = ReadingSourceConfigRepository(store)
        val user = DataStoreUserPreferencesRepository(store)
        user.pin("reader/Activity")
        user.setShowHomeStats(false)
        assertEquals(ReadingSourceConfig(), source.config.first())
        source.setTree(SourceFolder.KOReader, "content://tree/koreader")
        val id = source.config.first().sourceId
        assertNotNull(id)
        source.setTree(SourceFolder.Books, "content://tree/books")
        val saved = ReadingSourceConfigRepository(store).config.first()
        assertEquals(id, saved.sourceId)
        assertEquals("content://tree/koreader", saved.koreaderTreeUri)
        assertEquals("content://tree/books", saved.booksTreeUri)
        assertEquals(listOf("reader/Activity"), user.pinnedComponentIds.first())
        assertEquals(false, user.appPreferences.first().showHomeStats)
    }
}
