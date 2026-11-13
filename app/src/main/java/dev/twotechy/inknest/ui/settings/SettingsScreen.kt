package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.theme.InkLayoutTokens

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
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap),
        ) {
            state.error?.let { error ->
                item {
                    SettingsErrorPanel(error, actions.retry, actions.dismiss)
                }
            }
            if (state.preferences == null) {
                item { Text(stringResource(R.string.loading_settings)) }
            } else {
                when (page) {
                    SettingsPage.Overview -> item { SettingsOverview(actions.openPage) }

                    SettingsPage.Appearance -> item {
                        AppearanceSettingsContent(state, actions)
                    }

                    SettingsPage.ReadingData -> readingDataSettings(state, actions)

                    SettingsPage.Launcher -> item {
                        LauncherSettingsContent(
                            isDefaultLauncher,
                            actions.requestLauncher,
                            actions.systemSettings,
                        )
                    }

                    SettingsPage.About -> item { AboutSettingsContent(state.appUpdate, actions) }

                    SettingsPage.Updates -> item {
                        AppUpdatesContent(state.appUpdate, state.releaseNotes, actions.updates)
                    }

                    SettingsPage.Notices,
                    SettingsPage.LuaJLicense,
                    SettingsPage.MaterialSymbolsLicense,
                    SettingsPage.CommonMarkLicense,
                    -> item {
                        LicenseSettingsContent(page, state, actions)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.readingDataSettings(state: SettingsUiState, actions: SettingsActions) {
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
