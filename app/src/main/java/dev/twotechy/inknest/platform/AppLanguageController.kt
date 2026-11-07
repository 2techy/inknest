package dev.twotechy.inknest.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.twotechy.inknest.model.AppLanguage

// AppCompat owns persistence and bridges Android's per-app language settings.
internal class AppLanguageController {
    fun current(): AppLanguage = AppLanguage.fromLanguageTag(
        AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag(),
    )

    fun select(language: AppLanguage) {
        if (language != current()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}
