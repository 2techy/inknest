package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius

@Composable
internal fun AppsTopBar(
    state: AppsUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    onToggleViewMode: () -> Unit,
) {
    if (state.isSearchActive) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().heightIn(
                min = InkHeaderTokens.minHeight,
            ).border(
                InkBorder.default,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(InkRadius.medium),
            )
                .focusRequester(searchFocusRequester),

            placeholder = { Text(stringResource(R.string.search_apps_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(InkRadius.medium),
            trailingIcon = {
                HeaderIconButton(R.drawable.ic_close, stringResource(R.string.close_search)) {
                    onSearchActiveChanged(false)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
    } else {
        ScreenHeader(stringResource(R.string.apps_title)) {
            HeaderIconButton(R.drawable.ic_search, stringResource(R.string.search_apps)) {
                onSearchActiveChanged(true)
            }
            val grid = state.viewMode == AppsViewMode.Grid
            HeaderIconButton(
                if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                stringResource(if (grid) R.string.switch_to_list else R.string.switch_to_grid),
                onToggleViewMode,
            )
        }
    }
}
