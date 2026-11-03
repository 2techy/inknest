package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.data.preview.HomePreviewMode
import dev.twotechy.inknest.ui.components.SectionHeading
import dev.twotechy.inknest.ui.components.StatePanel

@Composable
internal fun SettingsScreen(
    mode: HomePreviewMode?,
    isDefaultLauncher: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onMode: (HomePreviewMode) -> Unit,
    onRequestLauncherRole: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }
        item {
            StatePanel(
                "Made for quiet screens",
                "E-Ink appearance · high contrast, static transitions and a minute clock.",
            )
        }

        // Launcher Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Default launcher")
                if (isDefaultLauncher) {
                    StatePanel(
                        "InkNest is your default launcher",
                        "Pressing Home brings you directly to your books and pinned apps.",
                        action = "System home settings",
                        onAction = onOpenAndroidSettings,
                    )
                } else {
                    StatePanel(
                        "Set InkNest as default home",
                        "Keep your reading front and center every time you unlock your device.",
                        action = "Set as default launcher",
                        onAction = onRequestLauncherRole,
                    )
                }
            }
        }

        if (mode != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading("Home preview", "DEBUG BUILD ONLY")
                    Text(
                        stringResource(dev.twotechy.inknest.R.string.preview_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HomePreviewMode.entries.forEach { choice ->
                        Text(
                            (if (choice == mode) "●  " else "○  ") + choice.name,
                            Modifier.fillMaxWidth().selectable(
                                selected = mode == choice,
                                role = Role.RadioButton,
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                                indication = null,
                                onClick = { onMode(choice) },
                            ).padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        item {
            Text(
                "InkNest  ${BuildConfig.VERSION_NAME}\n2techy",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
