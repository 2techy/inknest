package dev.twotechy.inknest.data.repository.preferences.impl

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.twotechy.inknest.data.repository.testPreferencesDataStore
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
        val dataStore = testPreferencesDataStore(testFile, testScope)
        return DataStoreUserPreferencesRepository(dataStore)
    }

    @Test fun displayModeReadsLegacyAndStableValuesWithSafeFallback() = testScope.runTest {
        val store = testPreferencesDataStore(
            File(tmpFolder.newFolder(), "display.preferences_pb"),
            testScope,
        )
        val repo = DataStoreUserPreferencesRepository(store)
        val key = stringPreferencesKey("display_mode")
        assertEquals(DisplayMode.Grayscale, repo.appPreferences.first().displayMode)
        val cases = mapOf(
            "EInk" to DisplayMode.Grayscale,
            "Standard" to DisplayMode.Color,
            "grayscale" to DisplayMode.Grayscale,
            "color" to DisplayMode.Color,
            "unknown" to DisplayMode.Grayscale,
            "" to DisplayMode.Grayscale,
        )
        cases.forEach { (stored, expected) ->
            store.edit { it[key] = stored }
            assertEquals(stored, expected, repo.appPreferences.first().displayMode)
        }
    }

    @Test fun displayModeWritesStableValuesAndPreservesOtherPreferences() = testScope.runTest {
        val store = testPreferencesDataStore(
            File(tmpFolder.newFolder(), "display-write.preferences_pb"),
            testScope,
        )
        val repo = DataStoreUserPreferencesRepository(store)
        repo.pin("reader/Activity")
        repo.setShowHomeStats(false)
        repo.setShowHomeHeatmap(false)
        repo.setShowHomePinnedApps(false)
        repo.completeOnboarding()
        val original = repo.appPreferences.first()
        listOf(
            DisplayMode.Color to "color",
            DisplayMode.Grayscale to "grayscale",
        ).forEach { (mode, stored) ->
            repo.setDisplayMode(mode)
            assertEquals(stored, store.data.first()[stringPreferencesKey("display_mode")])
            assertEquals(original.copy(displayMode = mode), repo.appPreferences.first())
            assertEquals(listOf("reader/Activity"), repo.pinnedComponentIds.first())
        }
    }

    @Test fun heatmapDefaultsEnabledAndPersistsIndependently() = testScope.runTest {
        val repo = createRepository()
        assertTrue(repo.appPreferences.first().showHomeHeatmap)
        repo.setShowHomeHeatmap(false)
        assertEquals(false, repo.appPreferences.first().showHomeHeatmap)
        repo.setShowHomeStats(false)
        repo.setShowHomeHeatmap(true)
        assertTrue(repo.appPreferences.first().showHomeHeatmap)
        assertEquals(false, repo.appPreferences.first().showHomeStats)
    }

    @Test fun repeatedWritesSurviveStoreRecreation() = runTest {
        val file = File(tmpFolder.newFolder(), "reopened.preferences_pb")
        val firstJob = Job(backgroundScope.coroutineContext[Job])
        try {
            val store = testPreferencesDataStore(
                file,
                CoroutineScope(backgroundScope.coroutineContext + firstJob),
            )
            val repository = DataStoreUserPreferencesRepository(store)
            repository.pin("reader/Activity")
            repository.setDisplayMode(DisplayMode.Color)
            repository.setShowHomeStats(false)
            repository.setOnboardingLauncherPending(true)
            repository.setShowHomeStats(true)
            repository.setShowHomeStats(false)
        } finally {
            firstJob.cancelAndJoin()
        }
        val secondJob = Job(backgroundScope.coroutineContext[Job])
        try {
            val store = testPreferencesDataStore(
                file,
                CoroutineScope(backgroundScope.coroutineContext + secondJob),
            )
            val repository = DataStoreUserPreferencesRepository(store)
            assertEquals(listOf("reader/Activity"), repository.pinnedComponentIds.first())
            assertEquals(false, repository.appPreferences.first().showHomeStats)
            assertEquals(DisplayMode.Color, repository.appPreferences.first().displayMode)
            assertTrue(repository.appPreferences.first().onboardingLauncherPending)
            repository.completeOnboarding()
            val completed = repository.appPreferences.first()
            assertTrue(completed.onboardingCompleted)
            assertEquals(false, completed.onboardingLauncherPending)
        } finally {
            secondJob.cancelAndJoin()
        }
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
        assertEquals(AppPreferences(), repo.appPreferences.first())
        repo.setDisplayMode(DisplayMode.Color)
        repo.setShowHomeStats(false)
        repo.setShowHomePinnedApps(false)
        repo.completeOnboarding()
        assertEquals(
            AppPreferences(
                DisplayMode.Color,
                false,
                false,
                true,
            ),
            repo.appPreferences.first(),
        )
        assertEquals(listOf("reader/Activity"), repo.pinnedComponentIds.first())
    }
}
