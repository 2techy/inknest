package dev.twotechy.inknest.model

data class AppPreferences(
    val displayMode: DisplayMode = DisplayMode.EInk,
    val showHomeStats: Boolean = true,
    val showHomePinnedApps: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val showHomeHeatmap: Boolean = true,
    val onboardingLauncherPending: Boolean = false,
)
