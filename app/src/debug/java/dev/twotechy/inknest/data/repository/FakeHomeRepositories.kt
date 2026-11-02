package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.preview.HomePreviewController
import dev.twotechy.inknest.data.preview.HomePreviewMode
import dev.twotechy.inknest.model.PinnedApp
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

// This entire file is debug-only; neither fixtures nor fake repositories reach release.
internal val sampleBook = ReadingItem(
    ReadingItemId("sample", "little-prince.epub"),
    "The Little Prince",
    "Antoine de Saint-Exupéry",
    42,
    "sample://little-prince",
)
internal val samplePins = listOf(
    PinnedApp("sample/koreader", "KOReader"),
    PinnedApp("sample/files", "Files"),
    PinnedApp("sample/settings", "Settings"),
)
internal val sampleStats = ReadingStats(35, 130)

internal class FakeReadingRepository :
    ReadingRepository,
    HomePreviewController {
    private val state = MutableStateFlow<ReadingState>(ReadingState.Content(sampleBook))
    override val reading = state.asStateFlow()
    override suspend fun refresh() = Unit

    override fun select(mode: HomePreviewMode) {
        state.value = when (mode) {
            HomePreviewMode.Content -> ReadingState.Content(sampleBook)

            HomePreviewMode.Empty -> ReadingState.Empty

            HomePreviewMode.Loading -> ReadingState.Loading

            HomePreviewMode.Error -> ReadingState.Error(
                "Your reading source is unavailable. Please try again.",
            )
        }
    }
}

internal class FakeInstalledAppsRepository : InstalledAppsRepository {
    override val pinnedApps = flowOf(samplePins)
}

internal class FakeReadingStatsRepository : ReadingStatsRepository {
    override val stats = flowOf(sampleStats)
}
