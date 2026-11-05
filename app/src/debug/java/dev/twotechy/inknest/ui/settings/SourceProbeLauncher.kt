package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LaunchResult
import dev.twotechy.inknest.platform.OpenResult
import dev.twotechy.inknest.platform.ReadingItemOpener

internal class SourceProbeLauncher(
    private val apps: AppLauncher,
    private val reader: ReadingItemOpener,
) {
    fun launchApp(app: AppInfo?): Boolean =
        app?.let { apps.launch(it.componentId) } == LaunchResult.Success

    suspend fun launchBook(config: ReadingSourceConfig, sourcePath: String): Boolean {
        val sourceId = config.sourceId ?: return false
        return reader.open(ReadingItemId(sourceId, sourcePath)) == OpenResult.Dispatched
    }
}
