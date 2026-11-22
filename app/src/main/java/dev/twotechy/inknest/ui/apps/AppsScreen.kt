package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.icons.AppIconLoader
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppsScreen(
    state: AppsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    iconLoader: AppIconLoader? = null,
    actions: AppsActions,
) {
    val activeQuery = state.searchQuery.takeIf { state.isSearchActive && it.isNotBlank() }
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        gesturesEnabled = !state.isSearchActive,
        refresh = RefreshAction(
            !state.isLoading && !state.isSearchActive,
            state.isRefreshing,
            actions.onRetry,
        ),
        header = {
            AppsTopBar(
                state,
                actions.onSearchQueryChanged,
                actions.onSearchActiveChanged,
                actions.onToggleViewMode,
            )
        },
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            AppsErrorBanner(state, actions.onRetry, actions.onDismissError)
            Box(Modifier.weight(1f)) {
                when {
                    state.isLoading -> StatePanel(
                        stringResource(R.string.loading_apps),
                        stringResource(R.string.loading_apps_detail),
                    )

                    state.allApps.isEmpty() && (state.error == null || activeQuery != null) ->
                        AppsEmptyState(activeQuery)

                    else -> AppsContent(state, iconLoader, actions.item)
                }
            }
        }
    }
}

@Composable
private fun AppsErrorBanner(state: AppsUiState, onRetry: () -> Unit, onDismiss: () -> Unit) {
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
private fun AppsEmptyState(activeQuery: String?) {
    if (activeQuery != null) {
        StatePanel(
            stringResource(R.string.no_apps_found),
            stringResource(R.string.no_apps_match, activeQuery),
        )
    } else {
        StatePanel(
            stringResource(R.string.no_launcher_apps),
            stringResource(R.string.no_launcher_apps_detail),
        )
    }
}
