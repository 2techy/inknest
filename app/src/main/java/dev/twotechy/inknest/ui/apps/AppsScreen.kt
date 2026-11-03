package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.StatePanel

@Composable
internal fun AppsScreen(
    state: AppsUiState,
    modifier: Modifier = Modifier,
    iconLoader: AppIconLoader? = null,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    onToggleViewMode: () -> Unit,
    onAppClick: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onMovePin: (String, Int) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(state.isSearchActive) {
        if (state.isSearchActive) searchFocus.requestFocus()
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Include inter-cell gaps and allow narrow windows instead of forcing three columns.
        val columns = ((maxWidth + 12.dp) / (84.dp + 12.dp)).toInt().coerceAtLeast(1)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppsTopBar(
                state,
                searchFocus,
                onSearchQueryChanged,
                onSearchActiveChanged,
                onToggleViewMode,
            )
            state.error?.let { message ->
                Column(Modifier.fillMaxWidth()) {
                    Text(message)
                    Row {
                        if (!state.canDismissError) {
                            TextButton(
                                onClick = onRetry,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        } else {
                            TextButton(
                                onClick = onDismissError,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    }
                }
            }
            when {
                state.isLoading -> StatePanel(
                    stringResource(R.string.loading_apps),
                    stringResource(R.string.loading_apps_detail),
                )

                state.allApps.isEmpty() && state.error == null -> {
                    val searching = state.isSearchActive && state.searchQuery.isNotBlank()
                    StatePanel(
                        stringResource(
                            if (searching) R.string.no_apps_found else R.string.no_launcher_apps,
                        ),
                        if (searching) {
                            stringResource(
                                R.string.no_apps_match,
                                state.searchQuery,
                            )
                        } else {
                            stringResource(R.string.no_launcher_apps_detail)
                        },
                    )
                }

                else -> AppsContent(state, columns, iconLoader, onAppClick, onTogglePin, onMovePin)
            }
        }
    }
}
