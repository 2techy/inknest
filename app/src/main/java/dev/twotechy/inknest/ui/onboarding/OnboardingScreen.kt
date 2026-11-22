package dev.twotechy.inknest.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.settings.DisplayModeChoices
import dev.twotechy.inknest.ui.settings.SettingsCommand
import dev.twotechy.inknest.ui.settings.SettingsSection
import dev.twotechy.inknest.ui.theme.InkButtonTokens
import dev.twotechy.inknest.ui.theme.InkOnboardingTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    contentPadding: PaddingValues,
) {
    val title = when (state.step) {
        0 -> R.string.setup_title
        1 -> R.string.setup_source_title
        else -> R.string.setup_launcher_title
    }
    val description = when (state.step) {
        0 -> R.string.setup_welcome
        1 -> R.string.setup_source_description
        else -> R.string.setup_launcher_description
    }
    ScreenScaffold(
        contentPadding = contentPadding,
        gesturesEnabled = false,
        header = {
            ScreenHeader(
                stringResource(title),
                if (state.step > 0 && !state.saving) actions.back else null,
            )
        },
        footer = { OnboardingFooter(state, actions) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            item {
                val readingStep = state.step == 1
                val spacing = if (readingStep) InkSpacing.sm else InkSpacing.lg
                val typography = MaterialTheme.typography
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    SetupProgress(state.step)
                    Text(
                        stringResource(description),
                        style = if (readingStep) typography.bodyMedium else typography.bodyLarge,
                    )
                }
            }
            item { SetupStepContent(state, actions) }
            state.errorMessage?.let { message ->
                item {
                    Text(
                        stringResource(message),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Text(
            stringResource(R.string.setup_step, step + 1),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(step + 1f, 1f..3f, 1)
            },
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
        ) {
            repeat(3) { index ->
                Box(
                    Modifier.weight(1f).height(InkOnboardingTokens.progressHeight).background(
                        if (index <= step) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun SetupStepContent(state: OnboardingUiState, actions: OnboardingActions) {
    when (state.step) {
        0 -> SettingsSection(stringResource(R.string.display_mode_heading)) {
            DisplayModeChoices(state.mode, !state.saving, actions.mode)
        }

        1 -> OnboardingReadingContent(state, actions.folders)

        else -> SettingsSection(stringResource(R.string.default_home_app)) {
            if (state.isDefaultLauncher) {
                Text(
                    stringResource(R.string.default_launcher_active),
                    Modifier.padding(vertical = InkSpacing.md),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    stringResource(R.string.setup_launcher_not_default),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SettingsCommand(
                    stringResource(R.string.request_default_launcher),
                    !state.saving,
                    actions.requestLauncher,
                )
            }
        }
    }
}

@Composable
private fun OnboardingFooter(state: OnboardingUiState, actions: OnboardingActions) {
    val primaryLabel = when {
        state.saving -> R.string.saving
        state.step == 2 -> R.string.finish_setup
        state.step == 1 && !state.hasReadingFolder -> R.string.setup_reading_later
        else -> R.string.next
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        ActionButton(
            stringResource(primaryLabel),
            Modifier.fillMaxWidth(),
            enabled = !state.saving,
            primary = true,
            onClick = if (state.step == 2) actions.finish else actions.next,
        )
        if (state.step == 0) SkipSetupAction(state.saving, actions.finish)
    }
}

@Composable
private fun SkipSetupAction(saving: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = InkButtonTokens.minHeight).clickable(
            enabled = !saving,
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ).padding(InkSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.skip_setup),
            style = MaterialTheme.typography.labelLarge,
            color = if (saving) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
