package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.HeaderIconPlacement
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkHomeTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun HomeHeader(actions: HomeActions) {
    val brand = stringResource(R.string.brand_name)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val brandWidth = with(density) {
            measurer.measure(brand, InkTextStyles.homeBrand, softWrap = false).size.width.toDp()
        }
        val fits = brandWidth + InkHomeTokens.logoSize + InkHeaderTokens.touchTarget * 2 +
            InkSpacing.sm * 2 <= maxWidth
        if (fits) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.minHeight),
                horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeIdentity(brand, Modifier.weight(1f))
                HomeHeaderActions(actions)
            }
        } else {
            Column {
                HomeIdentity(brand)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    HomeHeaderActions(actions)
                }
            }
        }
    }
}

@Composable
private fun HomeIdentity(brand: String, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = InkHeaderTokens.minHeight),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_inknest_logo),
            null,
            Modifier.size(InkHomeTokens.logoSize),
        )
        Text(brand, style = InkTextStyles.homeBrand)
    }
}

@Composable
private fun HomeHeaderActions(actions: HomeActions) {
    Row {
        HeaderIconButton(
            R.drawable.ic_search,
            stringResource(R.string.library_search),
            onClick = actions.searchLibrary,
        )
        HeaderIconButton(
            R.drawable.ic_settings_outline,
            stringResource(R.string.settings_title),
            placement = HeaderIconPlacement.Centered,
            onClick = actions.settings,
        )
    }
}
