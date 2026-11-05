package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.twotechy.inknest.ui.navigation.InkNestBottomBar
import dev.twotechy.inknest.ui.navigation.InkNestNavigationRail
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.theme.InkLayoutTokens

@Composable
internal fun AppScaffold(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    showNavigation: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val gate = remember { ContentGestureGate() }
    CompositionLocalProvider(LocalContentGestureGate provides gate) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= InkLayoutTokens.railBreakpoint
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (showNavigation && !useRail) InkNestBottomBar(selected, onSelect)
                },
            ) { insets ->
                Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                    if (showNavigation && useRail) InkNestNavigationRail(selected, onSelect)
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        BoxWithConstraints(
                            Modifier.widthIn(max = InkLayoutTokens.maxContentWidth).fillMaxSize()
                                .tabSwipe(selected, showNavigation, onSelect),
                        ) {
                            val gutter = when {
                                maxWidth < InkLayoutTokens.narrowBreakpoint -> {
                                    InkLayoutTokens.narrowGutter
                                }

                                maxWidth < InkLayoutTokens.railBreakpoint -> {
                                    InkLayoutTokens.compactGutter
                                }

                                else -> InkLayoutTokens.expandedGutter
                            }
                            content(
                                PaddingValues(
                                    start = gutter,
                                    end = gutter,
                                    top = InkLayoutTokens.topPadding,
                                    bottom = InkLayoutTokens.bottomPadding,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
