package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.readingIssueMessage
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun HomeReadingSection(state: ReadingSectionState, actions: HomeActions) {
    when (state) {
        ReadingSectionState.Loading -> HomeReadingNotice(
            stringResource(R.string.home_reading_loading),
            stringResource(R.string.home_reading_loading_detail),
        )

        ReadingSectionState.NotConfigured -> HomeReadingNotice(
            stringResource(R.string.home_connect_reader),
            stringResource(R.string.home_connect_reader_detail),
            onClick = actions.settings,
        )

        ReadingSectionState.Empty -> HomeReadingNotice(
            stringResource(R.string.home_reading_empty),
            stringResource(R.string.home_reading_empty_detail),
        )

        is ReadingSectionState.Error -> HomeReadingNotice(
            stringResource(R.string.home_reading_error),
            readingIssueMessage(state.issue),
            onClick = actions.settings,
        )

        is ReadingSectionState.Content -> Column(
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap),
        ) {
            Text(stringResource(R.string.home_now_reading), style = InkTextStyles.sectionTitle)
            ContinueReadingCard(state.current, actions.reading)
        }
    }
}
