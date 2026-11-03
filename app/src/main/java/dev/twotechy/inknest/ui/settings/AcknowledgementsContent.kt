package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AcknowledgementsContent(state: AcknowledgementsUiState, retry: () -> Unit) {
    when (state) {
        AcknowledgementsUiState.NotLoaded, AcknowledgementsUiState.Loading ->
            Text(stringResource(R.string.loading_notices))

        is AcknowledgementsUiState.Content -> Text(
            state.text,
            style = MaterialTheme.typography.bodyMedium,
        )

        AcknowledgementsUiState.Failed -> Column(
            verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            Text(stringResource(R.string.notices_unavailable))
            SettingsAction(stringResource(R.string.retry), onClick = retry)
        }
    }
}
