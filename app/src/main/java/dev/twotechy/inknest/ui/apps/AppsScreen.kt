package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.theme.InkAppGridTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppsScreen(
    state: AppsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
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
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        gesturesEnabled = !state.isSearchActive,
        refresh = RefreshAction(
            !state.isLoading && !state.isSearchActive,
            state.isRefreshing,
            onRetry,
        ),
        header = {
            AppsTopBar(
                state,
                searchFocus,
                onSearchQueryChanged,
                onSearchActiveChanged,
                onToggleViewMode,
            )
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cellWidth = InkAppGridTokens.minCellWidth * fontScale
            val gap = InkAppGridTokens.horizontalGap
            val columns = ((maxWidth + gap) / (cellWidth + gap)).toInt().coerceAtLeast(1)
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
            ) {
                AppsError(state, onRetry, onDismissError)
                Box(Modifier.weight(1f)) {
                    when {
                        state.isLoading -> StatePanel(
                            stringResource(R.string.loading_apps),
                            stringResource(R.string.loading_apps_detail),
                        )

                        state.allApps.isEmpty() && state.error == null -> AppsEmptyState(state)

                        else -> AppsContent(
                            state,
                            columns,
                            iconLoader,
                            onAppClick,
                            onTogglePin,
                            onMovePin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsError(state: AppsUiState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    if (state.error == null) return
    val label = if (state.canDismissError) R.string.dismiss else R.string.retry
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Text(
            stringResource(
                if (state.error == AppsError.SavePins) {
                    R.string.apps_pins_save_failed
                } else {
                    R.string.apps_refresh_failed
                },
            ),
        )
        ActionButton(
            stringResource(label),
            onClick = if (state.canDismissError) onDismiss else onRetry,
        )
    }
}

@Composable
private fun AppsEmptyState(state: AppsUiState) {
    val searching = state.isSearchActive && state.searchQuery.isNotBlank()
    val title = if (searching) R.string.no_apps_found else R.string.no_launcher_apps
    val detail = if (searching) {
        stringResource(R.string.no_apps_match, state.searchQuery)
    } else {
        stringResource(R.string.no_launcher_apps_detail)
    }
    StatePanel(stringResource(title), detail)
}
