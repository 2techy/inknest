package dev.twotechy.inknest.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createRepository(): DataStoreUserPreferencesRepository {
        val testFile = File(tmpFolder.newFolder(), "test_user_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        return DataStoreUserPreferencesRepository(dataStore)
    }

    @Test
    fun `pin changes preserve ordering and uniqueness`() = testScope.runTest {
        val repo = createRepository()
        assertTrue(repo.pinnedComponentIds.first().isEmpty())
        repo.pin("pkg.a/ActivityA")
        repo.pin("pkg.b/ActivityB")
        repo.pin("pkg.a/ActivityA")
        assertEquals(listOf("pkg.a/ActivityA", "pkg.b/ActivityB"), repo.pinnedComponentIds.first())
        repo.unpin("pkg.a/ActivityA")
        assertEquals(listOf("pkg.b/ActivityB"), repo.pinnedComponentIds.first())
    }

    @Test
    fun `reorder shifts component position`() = testScope.runTest {
        val repo = createRepository()
        repo.pin("pkg.a/ActivityA")
        repo.pin("pkg.b/ActivityB")
        repo.pin("pkg.c/ActivityC")

        repo.movePin("pkg.c/ActivityC", offset = -2)

        assertEquals(
            listOf("pkg.c/ActivityC", "pkg.a/ActivityA", "pkg.b/ActivityB"),
            repo.pinnedComponentIds.first(),
        )
    }

    @Test
    fun `pruneStalePins removes IDs not present in valid set`() = testScope.runTest {
        val repo = createRepository()
        repo.pin("pkg.a/ActivityA")
        repo.pin("pkg.b/ActivityB")
        repo.pin("pkg.c/ActivityC")

        repo.pruneStalePins(setOf("pkg.a/ActivityA", "pkg.c/ActivityC"))

        assertEquals(listOf("pkg.a/ActivityA", "pkg.c/ActivityC"), repo.pinnedComponentIds.first())
    }

    @Test
    fun `new settings default safely and edits preserve existing pins`() = testScope.runTest {
        val repo = createRepository()
        repo.pin("reader/Activity")
        assertEquals(dev.twotechy.inknest.model.AppPreferences(), repo.appPreferences.first())
        repo.setDisplayMode(dev.twotechy.inknest.model.DisplayMode.Standard)
        repo.setShowHomeStats(false)
        repo.setShowHomePinnedApps(false)
        repo.completeOnboarding()
        assertEquals(
            dev.twotechy.inknest.model.AppPreferences(
                dev.twotechy.inknest.model.DisplayMode.Standard,
                false,
                false,
                true,
            ),
            repo.appPreferences.first(),
        )
        assertEquals(listOf("reader/Activity"), repo.pinnedComponentIds.first())
    }
}
