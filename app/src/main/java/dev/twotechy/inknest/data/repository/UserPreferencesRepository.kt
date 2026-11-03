package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface UserPreferencesRepository {
    val appPreferences: Flow<AppPreferences>
    suspend fun setDisplayMode(mode: DisplayMode)
    suspend fun setShowHomeStats(show: Boolean)
    suspend fun setShowHomePinnedApps(show: Boolean)
    suspend fun completeOnboarding()

    val pinnedComponentIds: Flow<List<String>>
    val readError: Flow<String?> get() = flowOf(null)
    fun retryRead() = Unit

    suspend fun pin(componentId: String)
    suspend fun unpin(componentId: String)
    suspend fun movePin(componentId: String, offset: Int)
    suspend fun pruneStalePins(validComponentIds: Set<String>)
}
