package dev.twotechy.inknest.ui.library.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.BookReadingStatistics
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.components.formatReadingTime
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.components.readingIssueMessage
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun BookDetailsScreen(
    state: BookDetailsUiState,
    actions: BookDetailsActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    ScreenScaffold(
        modifier,
        contentPadding,
        header = {
            ScreenHeader(stringResource(R.string.library_details), actions.back, isDetail = true)
        },
        gesturesEnabled = false,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            val book = state.details.book
            if (book == null) {
                val message = if (state.details.loading) {
                    R.string.library_loading
                } else {
                    R.string.book_details_removed
                }
                Text(stringResource(message))
                ActionButton(stringResource(R.string.book_details_retry), onClick = actions.retry)
                return@Column
            }
            BookDetailsHero(book, state.zone, actions.open)
            Column {
                book.description?.let {
                    BookDescription(it, state.descriptionExpanded, actions.description)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap),
                ) {
                    HorizontalDivider()
                    BookDetailsStatistics(state.details.statistics, actions.retry)
                }
            }
            Column {
                HorizontalDivider()
                BookDetailsExpander(
                    R.string.book_details_history,
                    R.drawable.ic_nav_stats_outline,
                    state.historyExpanded,
                    actions.history,
                )
                if (state.historyExpanded) {
                    BookDetailsHistory(
                        state.details.statistics,
                        actions.page,
                    )
                }
                HorizontalDivider()
                BookDetailsExpander(
                    R.string.book_details_file,
                    R.drawable.ic_book_folders,
                    state.fileExpanded,
                    actions.file,
                )
                if (state.fileExpanded) BookFileInformation(book)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BookFileInformation(book: dev.twotechy.inknest.model.ReadingItem) {
    val availability = book.availability.detailsLabel()
    Column(
        Modifier.padding(top = InkSpacing.xs, bottom = InkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
    ) {
        Text(
            book.id.path.substringAfterLast('.').uppercase(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(availability), style = MaterialTheme.typography.bodyMedium)
        Text(
            book.id.path.removePrefix("primary:"),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun BookDescription(value: String, expanded: Boolean, toggle: () -> Unit) {
    var overflows by remember(value) { mutableStateOf(false) }
    Column(Modifier.padding(bottom = if (expanded || overflows) 0.dp else InkSpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
            Text(
                stringResource(R.string.book_details_description),
                style = InkTextStyles.sectionTitle,
            )
            Text(
                value,
                style = InkTextStyles.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            )
        }
        if (expanded || overflows) {
            BookDetailsExpander(
                if (expanded) R.string.book_details_show_less else R.string.book_details_show_more,
                null,
                expanded,
                toggle,
            )
        }
    }
}

@Composable
private fun BookDetailsExpander(label: Int, icon: Int?, expanded: Boolean, toggle: () -> Unit) {
    val description =
        stringResource(
            if (expanded) R.string.book_details_expanded else R.string.book_details_collapsed,
        )
    Row(
        Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget)
            .semantics { stateDescription = description }.quietClick(toggle),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(painterResource(it), null, Modifier.size(InkHeaderTokens.iconSize)) }
        Text(
            stringResource(label),
            if (icon == null) Modifier else Modifier.weight(1f),
            style = if (icon == null) InkTextStyles.labelLarge else InkTextStyles.sectionTitle,
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            null,
            Modifier.size(InkHeaderTokens.iconSize).rotate(if (expanded) -90f else 90f),
        )
    }
}

@Composable
private fun BookDetailsStatistics(statistics: BookReadingStatistics, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
        Text(stringResource(R.string.book_details_statistics), style = InkTextStyles.sectionTitle)
        if (statistics.status == BookStatisticsStatus.Ready) {
            val number = NumberFormat.getIntegerInstance(currentUiLocale())
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
            ) {
                BookMetric(
                    R.string.book_details_time,
                    formatReadingTime(statistics.seconds),
                    Modifier.weight(1f),
                )
                BookMetric(
                    R.string.book_details_pages,
                    statistics.pages?.let(number::format) ?: stringResource(R.string.stats_unknown),
                    Modifier.weight(1f),
                )
                BookMetric(
                    R.string.book_details_days,
                    number.format(statistics.readingDays),
                    Modifier.weight(1f),
                )
            }
        } else {
            Text(
                stringResource(
                    statistics.status.emptyLabel(),
                ),
                style = InkTextStyles.bodyMedium,
            )
        }
        statistics.error?.let { Text(readingIssueMessage(it), style = InkTextStyles.bodySmall) }
        if (statistics.error != null) {
            ActionButton(stringResource(R.string.book_details_retry), onClick = retry)
        }
    }
}

@Composable
private fun BookMetric(label: Int, value: String, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(InkRadius.large),
        border = BorderStroke(InkBorder.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = InkSpacing.xs, vertical = InkSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
        ) {
            Text(value, style = InkTextStyles.cardTitle, textAlign = TextAlign.Center)
            Text(
                stringResource(label),
                style = InkTextStyles.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BookDetailsHistory(statistics: BookReadingStatistics, page: (Int) -> Unit) {
    Column(
        Modifier.padding(top = InkSpacing.xs, bottom = InkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        val date = DateTimeFormatter.ofLocalizedDate(
            FormatStyle.MEDIUM,
        ).withLocale(currentUiLocale())
        if (statistics.history.isEmpty()) {
            Text(
                stringResource(statistics.status.emptyLabel()),
                style = InkTextStyles.bodyMedium,
            )
        }
        statistics.history.forEach { day ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
            ) {
                Text(date.format(day.date), Modifier.weight(1f), style = InkTextStyles.bodyMedium)
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatReadingTime(day.seconds), style = InkTextStyles.labelLarge)
                    day.pages?.let {
                        Text(
                            stringResource(
                                R.string.stats_pages_value,
                                NumberFormat.getIntegerInstance(currentUiLocale()).format(it),
                            ),
                            style = InkTextStyles.bodySmall,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
        if (statistics.historyPageCount > 1) BookHistoryPager(statistics, page)
    }
}

@Composable
private fun BookHistoryPager(statistics: BookReadingStatistics, page: (Int) -> Unit) {
    val enabled = statistics.canChangeHistoryPage
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BookHistoryPageButton(
            stringResource(R.string.book_details_previous_page),
            backwards = true,
            enabled = enabled && statistics.historyPage > 0,
        ) { page(statistics.historyPage - 1) }
        Text(
            stringResource(
                R.string.book_details_history_page,
                statistics.historyPage + 1,
                statistics.historyPageCount,
            ),
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = InkTextStyles.bodySmall,
        )
        BookHistoryPageButton(
            stringResource(R.string.book_details_next_page),
            backwards = false,
            enabled = enabled && statistics.hasMore,
        ) { page(statistics.historyPage + 1) }
    }
}

@Composable
private fun BookHistoryPageButton(
    label: String,
    backwards: Boolean,
    enabled: Boolean,
    click: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(InkHeaderTokens.touchTarget).clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = null,
            indication = null,
            onClick = click,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            label,
            Modifier.size(InkHeaderTokens.iconSize).rotate(if (backwards != rtl) 180f else 0f),
            tint = if (enabled) colors.onSurface else colors.outline,
        )
    }
}

private fun BookStatisticsStatus.emptyLabel(): Int = when (this) {
    BookStatisticsStatus.Loading -> R.string.book_details_stats_loading
    BookStatisticsStatus.Unavailable -> R.string.book_details_stats_unavailable
    BookStatisticsStatus.Unlinked -> R.string.book_details_stats_unlinked
    BookStatisticsStatus.Ready -> R.string.book_details_no_history
}
