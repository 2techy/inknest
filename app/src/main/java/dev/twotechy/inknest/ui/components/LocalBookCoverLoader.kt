package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import dev.twotechy.inknest.platform.covers.BookCoverLoader

internal val LocalBookCoverLoader = staticCompositionLocalOf<BookCoverLoader?> { null }
