package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles
import dev.twotechy.inknest.ui.theme.LocalAppPreferences

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
    val preferences = LocalAppPreferences.current
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        header = {
            ScreenHeader(stringResource(R.string.brand_name))
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap),
        ) {
            when (state) {
                HomeUiState.Loading -> {
                    item { DateTimeHeader(clockTick, null, preferences.showHomeStats) }
                    item {
                        HomeReadingNotice(
                            stringResource(R.string.home_reading_loading),
                            stringResource(R.string.home_reading_loading_detail),
                        )
                    }
                }

                is HomeUiState.Loaded -> {
                    item {
                        DateTimeHeader(clockTick, state.stats, preferences.showHomeStats)
                    }

                    // Reading section
                    when (val reading = state.reading) {
                        ReadingSectionState.Loading -> item {
                            HomeReadingNotice(
                                stringResource(R.string.home_reading_loading),
                                stringResource(R.string.home_reading_loading_detail),
                            )
                        }

                        ReadingSectionState.Empty -> item {
                            HomeReadingNotice(
                                stringResource(R.string.home_connect_reader),
                                stringResource(R.string.home_connect_reader_detail),
                                onClick = onSettings,
                            )
                        }

                        is ReadingSectionState.Error -> item {
                            HomeReadingNotice(
                                stringResource(R.string.home_reading_error),
                                reading.message,
                                onClick = onSettings,
                            )
                        }

                        is ReadingSectionState.Content -> item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    InkLayoutTokens.sectionContentGap,
                                ),
                            ) {
                                Text(
                                    stringResource(R.string.home_now_reading),
                                    style = InkTextStyles.sectionTitle,
                                )
                                ContinueReadingCard(reading.current, onReadingAction)
                            }
                        }
                    }

                    // Hiding this section does not remove persisted pins.
                    if (preferences.showHomePinnedApps) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    InkLayoutTokens.sectionContentGap,
                                ),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.home_pinned_apps),
                                        style = InkTextStyles.sectionTitle,
                                        modifier = Modifier.weight(1f),
                                    )
                                    HeaderIconButton(
                                        R.drawable.ic_chevron_right,
                                        stringResource(R.string.home_all_apps),
                                        onNavigateToApps,
                                    )
                                }
                                if (state.pinnedApps.isEmpty()) {
                                    HomeReadingNotice(
                                        stringResource(R.string.home_choose_apps),
                                        stringResource(R.string.home_choose_apps_detail),
                                        icon = R.drawable.ic_nav_apps_outline,
                                        onClick = onNavigateToApps,
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
    }
}
