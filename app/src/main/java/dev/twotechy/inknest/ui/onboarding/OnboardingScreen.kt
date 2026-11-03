package dev.twotechy.inknest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.settings.DisplayModeChoices
import dev.twotechy.inknest.ui.settings.SettingsAction
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun OnboardingScreen(
    step: Int,
    mode: DisplayMode,
    saving: Boolean,
    saveError: Boolean,
    preferenceError: Boolean,
    isDefaultLauncher: Boolean,
    onMode: (DisplayMode) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onRoleRequest: () -> Unit,
    contentPadding: PaddingValues,
) {
    val title = when (step) {
        0 -> R.string.setup_title
        1 -> R.string.setup_source_title
        else -> R.string.setup_launcher_title
    }
    val primaryLabel = when {
        saving -> R.string.saving
        step == 2 -> R.string.finish_setup
        else -> R.string.next
    }
    ScreenScaffold(
        contentPadding = contentPadding,
        header = { ScreenHeader(stringResource(title), if (step > 0 && !saving) onBack else null) },
        footer = {
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
            ) {
                ActionButton(
                    stringResource(R.string.skip_setup),
                    Modifier.weight(1f).fillMaxHeight(),
                    enabled = !saving,
                    onClick = onFinish,
                )
                ActionButton(
                    stringResource(primaryLabel),
                    Modifier.weight(1f).fillMaxHeight(),
                    enabled = !saving,
                    primary = true,
                    onClick = if (step == 2) onFinish else onNext,
                )
            }
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            item {
                Text(
                    stringResource(R.string.setup_step, step + 1),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            item {
                Text(
                    stringResource(
                        when (step) {
                            0 -> R.string.setup_welcome
                            1 -> R.string.setup_source_description
                            else -> R.string.setup_launcher_description
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (step == 0) item { DisplayModeChoices(mode, !saving, onMode) }
            if (step == 2) {
                item {
                    if (isDefaultLauncher) {
                        Text(stringResource(R.string.default_launcher_active))
                    } else {
                        SettingsAction(
                            stringResource(R.string.request_default_launcher),
                            !saving,
                            onRoleRequest,
                        )
                    }
                }
            }
            if (saveError || preferenceError) {
                item {
                    val errorLabel = if (saveError) {
                        R.string.setup_save_error
                    } else {
                        R.string.preferences_save_error
                    }
                    Text(stringResource(errorLabel))
                }
            }
        }
    }
}
