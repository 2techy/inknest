package dev.twotechy.inknest.ui.settings

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.constrainHeight
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkUpdateTokens
import java.time.Instant

private data class UpdateMetadataField(val label: String, val value: String)

@Composable
internal fun UpdateVersionGrid(release: AppRelease?) {
    val fields = updateMetadata(release)
    val minimumWidth = InkUpdateTokens.metadataMinColumnWidth * LocalDensity.current.fontScale
    Layout(
        content = { fields.forEach { field -> UpdateMetadata(field) } },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val columnGap = InkSpacing.lg.roundToPx()
        val rowGap = InkSpacing.md.roundToPx()
        val columns = if (constraints.maxWidth >= minimumWidth.roundToPx() * 2 + columnGap) 2 else 1
        val columnWidth = (constraints.maxWidth - columnGap * (columns - 1)) / columns
        val cellConstraints = constraints.copy(
            minWidth = columnWidth,
            maxWidth = columnWidth,
            minHeight = 0,
        )
        val rows = measurables.map { it.measure(cellConstraints) }.chunked(columns)
        val height = rows.sumOf { row -> row.maxOf { it.height } } + rowGap * (rows.size - 1)
        layout(constraints.maxWidth, constraints.constrainHeight(height)) {
            var y = 0
            rows.forEach { row ->
                row.forEachIndexed { column, cell ->
                    cell.placeRelative(column * (columnWidth + columnGap), y)
                }
                y += row.maxOf { it.height } + rowGap
            }
        }
    }
}

@Composable
private fun updateMetadata(release: AppRelease?): List<UpdateMetadataField> = buildList {
    add(
        UpdateMetadataField(
            stringResource(R.string.updates_current_label),
            BuildConfig.VERSION_NAME,
        ),
    )
    add(
        UpdateMetadataField(
            stringResource(R.string.updates_latest_label),
            release?.version ?: stringResource(R.string.unknown_value),
        ),
    )
    if (release != null) {
        add(
            UpdateMetadataField(
                stringResource(R.string.updates_size_label),
                Formatter.formatShortFileSize(LocalContext.current, release.size),
            ),
        )
        add(
            UpdateMetadataField(
                stringResource(R.string.updates_date_label),
                publishedDate(release),
            ),
        )
    }
}

@Composable
private fun publishedDate(release: AppRelease): String {
    val published = remember(release.publishedAt) {
        runCatching { Instant.parse(release.publishedAt).toEpochMilli() }.getOrNull()
    }
        ?: return stringResource(R.string.unknown_value)
    return DateUtils.formatDateTime(
        LocalContext.current,
        published,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
    )
}

@Composable
private fun UpdateMetadata(field: UpdateMetadataField) {
    Column(
        Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
    ) {
        Text(
            field.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            field.value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
