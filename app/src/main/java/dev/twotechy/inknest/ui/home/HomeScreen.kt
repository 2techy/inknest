package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.clock.rememberMinuteClock
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ReadingSyncNotice
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.LocalAppPreferences

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    iconLoader: AppIconLoader?,
    modifier: Modifier = Modifier,
) {
    val preferences = LocalAppPreferences.current
    val loaded = state as? HomeUiState.Loaded
    ScreenScaffold(
        modifier = modifier,
        contentPadding = PaddingValues(),
        header = {
            ScreenHeader(stringResource(R.string.brand_name)) {
                HeaderIconButton(
                    R.drawable.ic_settings_sync,
                    stringResource(R.string.refresh_reading),
                ) {
                    if (loaded?.sync?.running != true) actions.refresh()
                }
            }
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap),
        ) {
            item { HomeClockHeader(loaded?.stats, preferences.showHomeStats) }
            if (loaded == null) {
                item { HomeReadingSection(ReadingSectionState.Loading, actions) }
            } else {
                if (loaded.sync.running || loaded.sync.stale) {
                    item { ReadingSyncNotice(loaded.sync) }
                }
                item { HomeReadingSection(loaded.reading, actions) }
                if (preferences.showHomePinnedApps) {
                    item { HomePinnedAppsSection(loaded.pinnedApps, iconLoader, actions) }
                }
            }
        }
    }
}

@Composable
private fun HomeClockHeader(stats: HomeReadingStats?, showStats: Boolean) {
    // Clock state is read here, so minute ticks invalidate only the visible header.
    DateTimeHeader(rememberMinuteClock(), stats, showStats)
}
