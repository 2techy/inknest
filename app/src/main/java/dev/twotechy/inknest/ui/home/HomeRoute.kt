package dev.twotechy.inknest.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.platform.clock.rememberMinuteClock

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tick = rememberMinuteClock()
    val context = LocalContext.current
    HomeScreen(tick, state, modifier, contentPadding, onDemoAction = {
        Toast.makeText(
            context,
            "Sample data only. Reading integration is coming later.",
            Toast.LENGTH_SHORT,
        ).show()
    }, onSettings = onSettings)
}
