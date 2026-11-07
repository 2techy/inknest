package dev.twotechy.inknest.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppLanguage
import dev.twotechy.inknest.platform.AppLanguageController

internal data class LanguageSelection(val current: AppLanguage, val open: () -> Unit)

@Composable
internal fun rememberLanguageSelection(
    controller: AppLanguageController,
    page: SettingsPage,
): LanguageSelection {
    var current by remember { mutableStateOf(controller.current()) }
    var visible by remember(page) { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { current = controller.current() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    if (visible) {
        LanguageDialog(current, onSelect = {
            visible = false
            current = it
            controller.select(it)
        }, onDismiss = { visible = false })
    }
    return LanguageSelection(current) { visible = true }
}

@Composable
internal fun LanguageDialog(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSelectionDialog(stringResource(R.string.language_title), onDismiss) {
        AppLanguage.entries.forEach { language ->
            SettingsChoice(stringResource(language.label()), language == current) {
                onSelect(language)
            }
        }
    }
}

internal fun AppLanguage.label(): Int = when (this) {
    AppLanguage.System -> R.string.language_system
    AppLanguage.English -> R.string.language_english
    AppLanguage.Vietnamese -> R.string.language_vietnamese
}
