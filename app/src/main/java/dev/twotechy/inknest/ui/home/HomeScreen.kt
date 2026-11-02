package dev.twotechy.inknest.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.ui.components.SectionHeading
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.components.quietClick

@Composable
internal fun HomeScreen(
    clockTick: Long,
    state: HomeUiState,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onDemoAction: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { DateTimeHeader(clockTick, (state as? HomeUiState.Content)?.stats) }
        when (state) {
            HomeUiState.Loading -> item {
                StatePanel("Gathering your books", "Loading your reading space…")
            }

            HomeUiState.Empty -> item {
                StatePanel(
                    "A little space for reading",
                    "Connect KOReader to bring your books here.",
                    "Open settings",
                    onSettings,
                )
            }

            is HomeUiState.Error -> item {
                StatePanel("Unable to load books", state.message, "Open settings", onSettings)
            }

            is HomeUiState.Content -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeading("Continue reading")
                        ContinueReadingCard(state.current, onDemoAction)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeading("Pinned apps")
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.pinnedApps.forEach { name ->
                                Column(
                                    Modifier.weight(
                                        1f,
                                    ).widthIn(min = 76.dp).quietClick(onDemoAction).padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        Modifier.size(48.dp).border(
                                            1.25.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp),
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            name.take(1),
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 24.sp,
                                        )
                                    }
                                    Text(
                                        name,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
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
