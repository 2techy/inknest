package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.AppIcon
import dev.twotechy.inknest.ui.components.SectionHeading
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.components.quietClick

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
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                state.pinnedApps.forEach { pin ->
                                    HomePinItem(
                                        pin = pin,
                                        iconLoader = iconLoader,
                                        onClick = { onAppClick(pin.componentId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePinItem(pin: HomePinUiModel, iconLoader: AppIconLoader?, onClick: () -> Unit) {
    Column(
        Modifier
            .widthIn(min = 76.dp)
            .quietClick(onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .border(
                    1.25.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(pin.componentId, pin.label, pin.iconVersion, iconLoader, Modifier.size(36.dp))
        }
        Text(
            pin.label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
