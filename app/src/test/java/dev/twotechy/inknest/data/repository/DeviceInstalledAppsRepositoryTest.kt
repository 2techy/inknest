package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.InstalledAppsDataSource
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceInstalledAppsRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val sampleApps = listOf(
        AppInfo("pkg.a/ActivityA", "Alpha"),
        AppInfo("pkg.b/ActivityB", "Beta"),
        AppInfo("pkg.c/ActivityC", "Gamma"),
    )

    private class FakePreferencesRepository : UserPreferencesRepository {
        val pins = MutableStateFlow<List<String>>(emptyList())
        override val appPreferences = kotlinx.coroutines.flow.flowOf(
            dev.twotechy.inknest.model.AppPreferences(),
        )
        override suspend fun setDisplayMode(mode: dev.twotechy.inknest.model.DisplayMode) = Unit
        override suspend fun setShowHomeStats(show: Boolean) = Unit
        override suspend fun setShowHomePinnedApps(show: Boolean) = Unit
        override suspend fun completeOnboarding() = Unit
        override val pinnedComponentIds = pins
        var prunedValidIds: Set<String>? = null

        override suspend fun pin(componentId: String) {
            pins.value = pins.value + componentId
        }

        override suspend fun unpin(componentId: String) {
            pins.value = pins.value - componentId
        }

        override suspend fun movePin(componentId: String, offset: Int) {
            val list = pins.value.toMutableList()
            val fromIndex = list.indexOf(componentId)
            val item = list.removeAt(fromIndex)
            list.add(fromIndex + offset, item)
            pins.value = list
        }

        override suspend fun pruneStalePins(validComponentIds: Set<String>) {
            prunedValidIds = validComponentIds
            pins.value = pins.value.filter { validComponentIds.contains(it) }
        }
    }

    private class FakeDataSource(var apps: List<AppInfo>) : InstalledAppsDataSource {
        var fail = false
        override suspend fun loadInstalledApps(): List<AppInfo> {
            if (fail) throw IOException("Package query failed")
            return apps
        }
    }

    @Test
    fun `resolves pinned apps using installed apps and auto-prunes stale pins`() =
        testScope.runTest {
            val prefsRepo = FakePreferencesRepository()
            prefsRepo.pins.value =
                listOf("pkg.a/ActivityA", "pkg.uninstalled/Activity", "pkg.b/ActivityB")

            val dataSource = FakeDataSource(sampleApps)
            val repo = DeviceInstalledAppsRepository(
                dataSource = dataSource,
                preferencesRepository = prefsRepo,
                applicationScope = backgroundScope,
            )

            // Initial reload was triggered in init
            val pinned = repo.pinnedApps.first()
            assertEquals(
                listOf(
                    PinnedApp("pkg.a/ActivityA", "Alpha"),
                    PinnedApp("pkg.b/ActivityB", "Beta"),
                ),
                pinned,
            )

            // Check that stale pin "pkg.uninstalled/Activity" was pruned
            assertEquals(listOf("pkg.a/ActivityA", "pkg.b/ActivityB"), prefsRepo.pins.value)
        }

    @Test
    fun `query failure retains catalog and persisted pins then recovers`() = testScope.runTest {
        val prefs = FakePreferencesRepository().apply { pins.value = listOf("pkg.a/ActivityA") }
        val source = FakeDataSource(sampleApps)
        val repo = DeviceInstalledAppsRepository(source, prefs, backgroundScope)
        val originalPins = prefs.pins.value
        source.fail = true
        repo.reload()
        assertEquals(sampleApps, repo.installedApps.first())
        assertEquals(originalPins, prefs.pins.value)
        assertNotNull(repo.error.first())
        source.fail = false
        repo.reload()
        assertNull(repo.error.first())
    }

    @Test
    fun `first query failure does not prune any saved pins`() = testScope.runTest {
        val prefs = FakePreferencesRepository().apply { pins.value = listOf("pkg.a/ActivityA") }
        val source = FakeDataSource(emptyList()).apply { fail = true }
        val repo = DeviceInstalledAppsRepository(source, prefs, backgroundScope)
        assertEquals(listOf("pkg.a/ActivityA"), prefs.pins.value)
        assertNull(prefs.prunedValidIds)
        assertNotNull(repo.error.first())
    }

    @Test
    fun `empty successful query prunes genuinely removed apps`() = testScope.runTest {
        val prefs = FakePreferencesRepository().apply { pins.value = listOf("pkg.a/ActivityA") }
        val source = FakeDataSource(sampleApps)
        val repo = DeviceInstalledAppsRepository(source, prefs, backgroundScope)
        source.apps = emptyList()
        repo.reload()
        assertEquals(emptyList<String>(), prefs.pins.value)
        assertNull(repo.error.first())
    }

    @Test
    fun `replacing package is retained until final package query completes`() = testScope.runTest {
        val prefs = FakePreferencesRepository().apply { pins.value = listOf("pkg.a/ActivityA") }
        val source = FakeDataSource(sampleApps)
        val repo = DeviceInstalledAppsRepository(source, prefs, backgroundScope)
        source.apps = sampleApps.drop(1)
        repo.reload(setOf("pkg.a"))
        assertEquals(listOf("pkg.a/ActivityA"), prefs.pins.value)
        assertEquals("Alpha", repo.pinnedApps.first().single().label)
        source.apps =
            sampleApps.map {
                if (it.componentId ==
                    "pkg.a/ActivityA"
                ) {
                    it.copy(label = "Updated", iconVersion = 2)
                } else {
                    it
                }
            }
        repo.reload()
        assertEquals(PinnedApp("pkg.a/ActivityA", "Updated", 2), repo.pinnedApps.first().single())
    }
}
