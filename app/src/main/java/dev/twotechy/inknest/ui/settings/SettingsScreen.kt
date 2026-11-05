package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceStatus
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    page: SettingsPage,
    isDefaultLauncher: Boolean,
    actions: SettingsActions,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        header = {
            ScreenHeader(
                stringResource(page.title),
                onBack = if (page == SettingsPage.Overview) null else actions.back,
                isDetail = page != SettingsPage.Overview,
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            state.error?.let { error ->
                item {
                    SettingsErrorPanel(
                        error,
                        actions.retry,
                        actions.dismiss,
                    )
                }
            }
            if (state.preferences == null) {
                item { Text(stringResource(R.string.loading_settings)) }
            } else {
                when (page) {
                    SettingsPage.Overview -> item { SettingsOverview(actions.openPage) }

                    SettingsPage.Appearance -> {
                        item {
                            SettingsSection(stringResource(R.string.display_mode_heading)) {
                                DisplayModeChoices(
                                    state.preferences.displayMode,
                                    !state.saving,
                                    actions.displayMode,
                                )
                            }
                        }
                        item {
                            SettingsSection(stringResource(R.string.home_options_heading)) {
                                SettingsToggle(
                                    stringResource(R.string.mini_stats),
                                    stringResource(R.string.mini_stats_description),
                                    state.preferences.showHomeStats,
                                    !state.saving,
                                    actions.showStats,
                                )
                                SettingsToggle(
                                    stringResource(R.string.home_pins),
                                    stringResource(R.string.home_pins_description),
                                    state.preferences.showHomePinnedApps,
                                    !state.saving,
                                    actions.showPins,
                                )
                            }
                        }
                    }

                    SettingsPage.ReadingData -> {
                        item {
                            SettingsSection(stringResource(R.string.reading_source_heading)) {
                                SourceSettingsContent(state, actions)
                            }
                        }
                        item {
                            SettingsSection(stringResource(R.string.reading_cache_heading)) {
                                DataSettingsContent(state, actions)
                            }
                        }
                    }

                    SettingsPage.Launcher -> item {
                        LauncherSettingsContent(
                            isDefaultLauncher,
                            actions.requestLauncher,
                            actions.systemSettings,
                        )
                    }

                    SettingsPage.About -> item { AboutSettingsContent(actions) }

                    SettingsPage.Notices -> item {
                        AcknowledgementsContent(
                            state.acknowledgements,
                            actions.retryAcknowledgements,
                        )
                    }
                }
            }
        }
    }
}
