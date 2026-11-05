package dev.twotechy.inknest.ui.onboarding

import dev.twotechy.inknest.model.DisplayMode

internal data class OnboardingUiState(
    val step: Int,
    val mode: DisplayMode,
    val saving: Boolean,
    val saveError: Boolean,
    val preferenceError: Boolean,
    val isDefaultLauncher: Boolean,
)

internal data class OnboardingActions(
    val mode: (DisplayMode) -> Unit,
    val next: () -> Unit,
    val back: () -> Unit,
    val finish: () -> Unit,
    val requestLauncher: () -> Unit,
)
