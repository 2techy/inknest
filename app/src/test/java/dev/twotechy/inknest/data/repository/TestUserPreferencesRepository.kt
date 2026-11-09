package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import kotlinx.coroutines.flow.MutableStateFlow

internal class TestUserPreferencesRepository : UserPreferencesRepository {
    override val appPreferences = MutableStateFlow(AppPreferences())
    override val pinnedComponentIds = MutableStateFlow(listOf("reader/Activity"))
    var failure: Exception? = null
    private fun update(transform: (AppPreferences) -> AppPreferences) {
        failure?.let { throw it }
        appPreferences.value = transform(appPreferences.value)
    }
    override suspend fun setDisplayMode(mode: DisplayMode) = update { it.copy(displayMode = mode) }
    override suspend fun setShowHomeHeatmap(show: Boolean) =
        update { it.copy(showHomeHeatmap = show) }
    override suspend fun setShowHomeStats(show: Boolean) = update { it.copy(showHomeStats = show) }
    override suspend fun setShowHomePinnedApps(show: Boolean) =
        update { it.copy(showHomePinnedApps = show) }
    override suspend fun completeOnboarding() = update { it.copy(onboardingCompleted = true) }
    override suspend fun pin(componentId: String) = Unit
    override suspend fun unpin(componentId: String) = Unit
    override suspend fun movePin(componentId: String, offset: Int) = Unit
    override suspend fun pruneStalePins(validComponentIds: Set<String>) = Unit
}
