package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.twotechy.inknest.platform.LauncherModeController

@Composable
internal fun rememberDefaultLauncher(controller: LauncherModeController): State<Boolean> {
    val status = remember(controller) { mutableStateOf(controller.isDefaultLauncher()) }
    LifecycleResumeEffect(controller) {
        status.value = controller.isDefaultLauncher()
        onPauseOrDispose { }
    }
    return status
}
