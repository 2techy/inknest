package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.model.AppLanguage

internal data class LanguageSelection(val current: AppLanguage, val open: () -> Unit)
