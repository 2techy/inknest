package dev.twotechy.inknest.ui.onboarding

import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.settings.SourceConnectionActions

internal data class OnboardingActions(
    val mode: (DisplayMode) -> Unit,
    val next: () -> Unit,
    val back: () -> Unit,
    val finish: () -> Unit,
    val requestLauncher: () -> Unit,
    val connection: SourceConnectionActions,
)
