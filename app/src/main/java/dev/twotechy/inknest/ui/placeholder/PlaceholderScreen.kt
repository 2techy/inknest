package dev.twotechy.inknest.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination

@Composable
internal fun PlaceholderScreen(
    tab: TopLevelDestination,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val (title, description) = when (tab) {
        TopLevelDestination.Library ->
            "Your reading shelf" to
                "Books you read will find a home here. Library browsing is coming in a later milestone."

        TopLevelDestination.Stats ->
            "A habit, one page at a time" to
                "Your reading activity will appear here once a reading source is connected."

        else ->
            "Your everyday essentials" to
                "An app drawer and pinned apps are coming in a later milestone."
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Text(tab.name, style = MaterialTheme.typography.headlineLarge) }
        item { StatePanel(title, description) }
    }
}
