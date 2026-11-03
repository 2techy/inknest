package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnboardingViewModel(
    private val preferences: UserPreferencesRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val step = savedState.getStateFlow("setupStep", 0)
    private val mutableSaving = MutableStateFlow(false)
    val saving = mutableSaving.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val completed = MutableStateFlow(0)
    val completion = completed.asStateFlow()
    fun acknowledgeCompletion() {
        completed.value = 0
    }
    fun restart() {
        savedState["setupStep"] = 0
        mutableError.value = false
    }
    fun next() {
        savedState["setupStep"] = (step.value + 1).coerceAtMost(2)
    }
    fun back() {
        if (step.value == 0) finish() else savedState["setupStep"] = step.value - 1
    }
    fun finish() {
        if (mutableSaving.value) return
        mutableSaving.value = true
        mutableError.value = false
        viewModelScope.launch {
            try {
                preferences.completeOnboarding()
                completed.value += 1
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableError.value = true
            } finally {
                mutableSaving.value = false
            }
        }
    }
}
