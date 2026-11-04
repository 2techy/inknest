package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun HomePinnedAppsSection(
    pins: List<HomePinUiModel>,
    iconLoader: AppIconLoader?,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_pinned_apps),
                style = InkTextStyles.sectionTitle,
                modifier = Modifier.weight(1f),
            )
            HeaderIconButton(
                R.drawable.ic_chevron_right,
                stringResource(R.string.home_all_apps),
                actions.openApps,
            )
        }
        if (pins.isEmpty()) {
            HomeReadingNotice(
                stringResource(R.string.home_choose_apps),
                stringResource(R.string.home_choose_apps_detail),
                icon = R.drawable.ic_nav_apps_outline,
                onClick = actions.openApps,
            )
        } else {
            HomePinnedApps(pins, iconLoader, actions.launchApp)
        }
    }
}
