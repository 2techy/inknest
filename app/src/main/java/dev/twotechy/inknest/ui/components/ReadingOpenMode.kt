package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.ReadingItemOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@Composable
internal fun rememberDirectReadingMode(
    opener: ReadingItemOpener?,
    source: ReadingSourceConfigRepository?,
): Boolean {
    val config by (source?.config ?: flowOf(ReadingSourceConfig()))
        .collectAsStateWithLifecycle(ReadingSourceConfig())
    var resume by remember { mutableIntStateOf(0) }
    var direct by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resume++ }
    LaunchedEffect(config, resume) {
        direct = withContext(Dispatchers.IO) { opener?.isDirect(config) == true }
    }
    return direct
}
