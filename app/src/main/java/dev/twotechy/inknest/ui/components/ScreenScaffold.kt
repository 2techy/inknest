package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.twotechy.inknest.ui.theme.InkLayoutTokens

// Window/system insets belong to AppScaffold; feature screens only apply this content gutter.
@Composable
internal fun ScreenScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.headerContentGap),
    ) {
        header()
        Box(Modifier.weight(1f), content = content)
        footer?.invoke()
    }
}
