package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
internal fun currentUiLocale(): Locale {
    val tags = LocalConfiguration.current.locales.toLanguageTags()
    return remember(tags) { resolveUiLocale(tags) }
}

internal fun resolveUiLocale(tags: String): Locale = tags.split(',')
    .asSequence().map(Locale::forLanguageTag)
    .firstOrNull { it.language == "en" || it.language == "vi" } ?: Locale.ENGLISH
