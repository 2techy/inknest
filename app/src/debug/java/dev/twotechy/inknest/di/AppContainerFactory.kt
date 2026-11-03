package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.repository.FakeHomePreviewRepository
import dev.twotechy.inknest.data.repository.MockSourceSettingsRepository

internal fun createAppContainer(context: Context): AppContainer {
    val preview = FakeHomePreviewRepository()
    val sources = MockSourceSettingsRepository()
    return createRuntimeContainer(context, preview, preview, preview, sources, sources)
}
