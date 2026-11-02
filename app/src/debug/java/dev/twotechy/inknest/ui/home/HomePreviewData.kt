package dev.twotechy.inknest.ui.home

// Only compiled into debug builds. Release cannot load these fixtures.
fun homeStateFor(mode: String): HomeUiState = when (mode) {
    "Loading" -> HomeUiState.Loading
    "Empty" -> HomeUiState.Empty
    "Error" -> HomeUiState.Error("Your reading source is unavailable. Please try again.")
    else -> HomeUiState.Content(
        current = BookPreview("1", "The Little Prince", "Antoine de Saint-Exupéry", 42, "LE PETIT\nPRINCE"),
        pinnedApps = listOf("KOReader", "Files", "Settings"),
        stats = HomeReadingStats(todayMinutes = 35, weekMinutes = 130),
    )
}
