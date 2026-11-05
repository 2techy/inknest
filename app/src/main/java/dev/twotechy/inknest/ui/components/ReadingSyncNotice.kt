package dev.twotechy.inknest.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus

@Composable
internal fun ReadingSyncNotice(sync: ReadingSyncStatus) {
    val text = when {
        sync.running -> stringResource(R.string.reading_refreshing)

        sync.error != null -> stringResource(
            R.string.reading_cached_warning,
            readingIssueMessage(sync.error),
        )

        else -> stringResource(R.string.reading_cached)
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun readingIssueMessage(issue: ReadingSyncIssue): String = stringResource(
    when (issue) {
        ReadingSyncIssue.PermissionLost -> R.string.reading_permission_lost
        ReadingSyncIssue.MissingData -> R.string.reading_missing_data
        ReadingSyncIssue.InvalidData -> R.string.reading_invalid_data
        ReadingSyncIssue.Provider -> R.string.reading_provider_error
        ReadingSyncIssue.Cache -> R.string.reading_cache_error
    },
)
