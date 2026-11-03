package dev.twotechy.inknest.model

enum class DisplayMode { EInk, Standard }

data class AppPreferences(
    val displayMode: DisplayMode = DisplayMode.EInk,
    val showHomeStats: Boolean = true,
    val showHomePinnedApps: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
