package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsDayDetail
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ReadingStatsPager
import dev.twotechy.inknest.ui.components.ReadingStatsRow
import dev.twotechy.inknest.ui.components.ReadingSyncNotice
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.components.formatStatsAmount
import dev.twotechy.inknest.ui.components.readingIssueMessage
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun StatsDayScreen(
    state: StatsDayUiState,
    actions: StatsDayActions,
    contentPadding: PaddingValues = PaddingValues(),
) {
    ScreenScaffold(
        contentPadding = contentPadding,
        header = {
            ScreenHeader(stringResource(R.string.stats_day_details), actions.back, isDetail = true)
        },
        gesturesEnabled = false,
    ) {
        val booksScroll = rememberLazyListState()
        val statusScroll = rememberLazyListState()
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = if (state.detail.amount == null) statusScroll else booksScroll,
        ) {
            item {
                val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(currentUiLocale()).format(state.day)
                Text(date, style = MaterialTheme.typography.titleLarge)
            }
            dayStatus(state.detail, actions.retry)
            dayBooks(state, actions)
        }
    }
}

private fun LazyListScope.dayStatus(detail: StatisticsDayDetail, retry: () -> Unit) {
    val notice = detail.sync.takeIf { it.stale && !it.running }
    if (notice != null) daySection { ReadingSyncNotice(notice) }
    if (detail.cacheError || detail.sync.error == ReadingSyncIssue.Cache) {
        daySection {
            if (notice?.error != ReadingSyncIssue.Cache) {
                Text(readingIssueMessage(ReadingSyncIssue.Cache))
            }
            ActionButton(stringResource(R.string.book_details_retry), onClick = retry)
        }
    } else if (detail.amount == null) {
        daySection {
            val label = if (detail.loading) {
                R.string.stats_calculating
            } else {
                R.string.stats_day_unavailable
            }
            Text(stringResource(label))
        }
    }
}

private fun LazyListScope.dayBooks(state: StatsDayUiState, actions: StatsDayActions) {
    val detail = state.detail
    val amount = detail.amount ?: return
    daySection { StatsDayTotals(amount) }
    daySection(topSpacing = InkSpacing.xl) {
        HorizontalDivider()
        Text(stringResource(R.string.stats_day_books), style = MaterialTheme.typography.titleMedium)
    }
    if (detail.books.isEmpty()) {
        daySection {
            val label = if (amount.seconds == 0L) {
                R.string.stats_day_empty
            } else {
                R.string.stats_day_empty_page
            }
            Text(stringResource(label))
        }
    }
    val hasLinkedBooks = detail.books.any { it.libraryItemId != null }
    items(detail.books, key = { it.id }) { book ->
        ReadingStatsRow(
            book.title ?: stringResource(R.string.stats_day_unknown_book),
            book.amount,
            alignChevrons = hasLinkedBooks,
            onClick = book.libraryItemId?.let { itemId -> { actions.openBook(itemId) } },
        )
    }
    if (state.page > 0 || detail.hasMore) {
        daySection { StatsDayPager(state.page, detail.hasMore, actions.page) }
    }
}

@Composable
private fun StatsDayPager(page: Int, hasMore: Boolean, onPage: (Int) -> Unit) {
    ReadingStatsPager(
        label = stringResource(R.string.stats_day_page, page + 1),
        onPrevious = if (page > 0) ({ onPage(page - 1) }) else null,
        onNext = if (hasMore) ({ onPage(page + 1) }) else null,
    )
}

private fun LazyListScope.daySection(
    topSpacing: Dp = InkLayoutTokens.sectionContentGap,
    content: @Composable () -> Unit,
) {
    item {
        Column(
            Modifier.padding(top = topSpacing),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
        ) {
            content()
        }
    }
}

@Composable
private fun StatsDayTotals(amount: StatisticsAmount) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        StatsDayTotal(
            formatStatsAmount(amount, StatisticsUnit.Time),
            stringResource(R.string.book_details_time),
            Modifier.weight(1f),
        )
        StatsDayTotal(
            formatStatsAmount(amount, StatisticsUnit.Pages),
            stringResource(R.string.book_details_pages),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatsDayTotal(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier.fillMaxHeight(),
        shape = RoundedCornerShape(InkRadius.large),
        border = BorderStroke(InkBorder.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(InkSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
