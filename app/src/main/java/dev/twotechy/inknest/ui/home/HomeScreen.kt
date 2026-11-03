package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.SectionHeading
import dev.twotechy.inknest.ui.components.StatePanel

@Composable
internal fun HomeScreen(
    clockTick: Long,
    state: HomeUiState,
    modifier: Modifier,
    contentPadding: PaddingValues,
    iconLoader: AppIconLoader? = null,
    onReadingAction: () -> Unit,
    onAppClick: (String) -> Unit,
    onNavigateToApps: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (state) {
            HomeUiState.Loading -> {
                item { DateTimeHeader(clockTick, null) }
                item {
                    StatePanel("Gathering your books", "Loading your reading space…")
                }
            }

            is HomeUiState.Loaded -> {
                item { DateTimeHeader(clockTick, state.stats) }

                // Reading section
                when (val reading = state.reading) {
                    ReadingSectionState.Loading -> item {
                        StatePanel("Gathering your books", "Loading your reading space…")
                    }

                    ReadingSectionState.Empty -> item {
                        StatePanel(
                            "A little space for reading",
                            "Connect KOReader to bring your books here.",
                            "Open settings",
                            onSettings,
                        )
                    }

                    is ReadingSectionState.Error -> item {
                        StatePanel(
                            "Unable to load books",
                            reading.message,
                            "Open settings",
                            onSettings,
                        )
                    }

                    is ReadingSectionState.Content -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeading("Continue reading")
                            ContinueReadingCard(reading.current, onReadingAction)
                        }
                    }
                }

                // Pinned apps section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeading("Pinned apps")
                        if (state.pinnedApps.isEmpty()) {
                            StatePanel(
                                "No pinned apps yet",
                                "Pin your favorite reading or companion apps for quick access.",
                                "Browse apps",
                                onNavigateToApps,
                            )
                        } else {
                            HomePinnedApps(
                                pins = state.pinnedApps,
                                iconLoader = iconLoader,
                                onAppClick = onAppClick,
                            )
                        }
                    }
                }
            }
        }
    }
}
