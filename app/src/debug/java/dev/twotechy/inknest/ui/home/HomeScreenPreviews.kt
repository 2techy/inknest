package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.sampleBook
import dev.twotechy.inknest.data.repository.samplePins
import dev.twotechy.inknest.data.repository.sampleStats
import dev.twotechy.inknest.ui.theme.InkNestTheme

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun HomeContentPreview() =
    PreviewHome(ReadingState.Content(sampleBook).toHomeUiState(samplePins, sampleStats))

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() =
    PreviewHome(ReadingState.Empty.toHomeUiState(samplePins, sampleStats))

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() = PreviewHome(HomeUiState.Loading)

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() = PreviewHome(
    ReadingState.Error(
        "Your reading source is unavailable. Please try again.",
    ).toHomeUiState(samplePins, sampleStats),
)

@Composable
private fun PreviewHome(state: HomeUiState) {
    InkNestTheme {
        HomeScreen(
            clockTick = 0L,
            state = state,
            modifier = Modifier,
            contentPadding = PaddingValues(16.dp),
            onReadingAction = {},
            onAppClick = {},
            onNavigateToApps = {},
            onSettings = {},
        )
    }
}
