package dev.twotechy.inknest.ui.placeholder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination

@Composable
internal fun PlaceholderScreen(
    tab: TopLevelDestination,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val library = tab == TopLevelDestination.Library
    val title =
        stringResource(if (library) R.string.library_empty_title else R.string.stats_empty_title)
    val description = stringResource(
        if (library) R.string.library_empty_description else R.string.stats_empty_description,
    )
    ScreenScaffold(modifier, contentPadding, header = { ScreenHeader(tab.name) }) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { StatePanel(title, description) }
        }
    }
}
