package dev.twotechy.inknest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.ForegroundRefreshCoordinator
import kotlinx.coroutines.launch

internal class AppViewModel(private val refresh: ForegroundRefreshCoordinator) : ViewModel() {
    fun onForeground() {
        viewModelScope.launch { refresh.refresh() }
    }
}
