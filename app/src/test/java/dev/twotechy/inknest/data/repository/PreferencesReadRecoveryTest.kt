package dev.twotechy.inknest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesReadRecoveryTest {
    private fun pins(value: String) =
        preferencesOf(DataStoreUserPreferencesRepository.KEY_PINNED_COMPONENT_IDS to value)

    @Test
    fun `read error keeps last pins and manual retry resumes updates`() = runTest {
        val snapshots = MutableStateFlow(Result.success(pins("a/A")))
        val store = object : DataStore<Preferences> {
            override val data = snapshots.map { it.getOrThrow() }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(snapshots.value.getOrThrow()).also {
                    snapshots.value = Result.success(it)
                }
        }
        val repository = DataStoreUserPreferencesRepository(store)
        val observed = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.pinnedComponentIds.collect { observed.add(it) }
        }
        assertEquals(listOf("a/A"), observed.last())
        snapshots.value = Result.failure(IOException("read failure"))
        runCurrent()
        assertNotNull(repository.readError.value)
        assertEquals(listOf("a/A"), observed.last())
        snapshots.value = Result.success(pins("b/B"))
        repository.retryRead()
        runCurrent()
        assertEquals(listOf("b/B"), observed.last())
        assertNull(repository.readError.value)
        snapshots.value = Result.success(pins("c/C"))
        runCurrent()
        assertEquals(listOf("c/C"), observed.last())
    }

    @Test
    fun `initial read error recovers without reporting an empty preference snapshot`() = runTest {
        val snapshots =
            MutableStateFlow<Result<Preferences>>(Result.failure(IOException("read failure")))
        val store = object : DataStore<Preferences> {
            override val data = snapshots.map { it.getOrThrow() }
            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences,
            ): Preferences = error("No write should be attempted")
        }
        val repository = DataStoreUserPreferencesRepository(store)
        val observed = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.pinnedComponentIds.collect { observed.add(it) }
        }
        assertEquals(emptyList<List<String>>(), observed)
        snapshots.value = Result.success(pins("saved/App"))
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(listOf(listOf("saved/App")), observed)
        assertNull(repository.readError.value)
    }
}
