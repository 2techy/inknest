package dev.twotechy.inknest.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LaunchResult

@Composable
internal fun rememberAppLaunchAction(
    launcher: AppLauncher,
    onUnavailable: () -> Unit,
): (String) -> Unit {
    val context = LocalContext.current
    val latestOnUnavailable by rememberUpdatedState(onUnavailable)
    return remember(launcher, context) {
        { componentId ->
            when (launcher.launch(componentId)) {
                LaunchResult.Success -> Unit

                LaunchResult.Unavailable -> {
                    latestOnUnavailable()
                    Toast.makeText(context, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
                }

                is LaunchResult.Denied -> {
                    Toast.makeText(context, R.string.app_launch_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
