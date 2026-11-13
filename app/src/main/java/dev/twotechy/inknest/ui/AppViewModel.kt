package dev.twotechy.inknest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.AppUpdateRepository
import dev.twotechy.inknest.data.repository.ForegroundRefreshCoordinator
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.model.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AppViewModel(
    private val refresh: ForegroundRefreshCoordinator,
    preferences: UserPreferencesRepository,
    private val installedApps: InstalledAppsRepository,
    private val updates: AppUpdateRepository? = null,
) : ViewModel() {
    val preferences = preferences.appPreferences.map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var appLocales: String? = null

    fun onLocalesChanged(locales: String) {
        val previous = appLocales
        appLocales = locales
        if (previous != null && previous != locales) {
            viewModelScope.launch { installedApps.reload() }
        }
    }

    fun onForeground() {
        viewModelScope.launch { refresh.refresh() }
    }

    suspend fun runUpdateChecks() {
        updates?.runForeground()
    }
}
