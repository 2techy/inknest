package dev.twotechy.inknest.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.SearchField

@Composable
internal fun LibraryHeader(state: LibraryUiState, actions: LibraryActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.options.searching) {
        if (state.options.searching) focus.requestFocus()
    }
    if (state.options.searching) {
        SearchField(
            state.options.query,
            stringResource(R.string.library_search_hint),
            focus,
            actions.query,
            onClose = { actions.searching(false) },
        )
    } else {
        ScreenHeader(
            stringResource(R.string.library_title),
            onBack = if (state.options.authorKey !=
                null
            ) {
                actions.backToAuthors
            } else {
                null
            },
        ) {
            HeaderIconButton(R.drawable.ic_search, stringResource(R.string.library_search)) {
                actions.searching(true)
            }
            LibraryOptionsMenu(state, actions)
        }
    }
}
