package dev.twotechy.inknest.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.ui.components.ReadingOpenEffect
import dev.twotechy.inknest.ui.components.ReadingOpenErrorActions
import dev.twotechy.inknest.ui.library.LibraryViewModel
import dev.twotechy.inknest.ui.library.details.BookDetailsActions
import dev.twotechy.inknest.ui.library.details.BookDetailsScreen
import dev.twotechy.inknest.ui.library.details.BookDetailsViewModel
import dev.twotechy.inknest.ui.stats.StatsDayNavigation
import dev.twotechy.inknest.ui.stats.StatsDayRoute
import dev.twotechy.inknest.ui.stats.StatsViewModel

@Composable
internal fun DetailDestinations(
    stack: List<DetailDestination>,
    container: AppContainer,
    navigation: AppNavigationViewModel,
    padding: PaddingValues,
) {
    BackHandler(onBack = navigation::backFromDetail)
    val dayState = rememberSaveableStateHolder()
    when (val entry = stack.last()) {
        is DetailDestination.Book -> key(entry.id) {
            val details: BookDetailsViewModel = viewModel(
                viewModelStoreOwner = rememberViewModelStoreOwner(),
                factory = container.bookDetailsViewModelFactory,
            )
            LaunchedEffect(details, entry.id) { details.select(entry.id) }
            BookDetailsDestination(details, container, navigation, padding)
        }

        is DetailDestination.StatsDay -> dayState.SaveableStateProvider(entry.date.toEpochDay()) {
            val stats: StatsViewModel = viewModel(factory = container.statsViewModelFactory)
            StatsDayRoute(
                stats,
                StatsDayNavigation(entry.date, navigation::backFromDetail, navigation::openBook),
                padding,
            )
        }
    }
}

@Composable
private fun BookDetailsDestination(
    details: BookDetailsViewModel,
    container: AppContainer,
    navigation: AppNavigationViewModel,
    padding: PaddingValues,
) {
    val library: LibraryViewModel = viewModel(factory = container.libraryViewModelFactory)
    val state by details.uiState.collectAsStateWithLifecycle()
    val opening by library.bookOpening.state.collectAsStateWithLifecycle()
    val open = library.bookOpening.actions
    ReadingOpenEffect(
        opening,
        open,
        container.readingItemDispatcher,
        ReadingOpenErrorActions(navigation::openReadingDataSettings, library::refresh),
    )
    BookDetailsScreen(
        state,
        BookDetailsActions(
            navigation::backFromDetail,
            { state.details.book?.id?.let(open.open) },
            details::toggleDescription,
            details::toggleHistory,
            details::toggleFile,
            details::historyPage,
            details::retry,
        ),
        Modifier.fillMaxSize(),
        padding,
    )
}
