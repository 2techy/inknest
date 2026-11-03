package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.repository.FakeHomePreviewRepository

internal fun createAppContainer(context: Context): AppContainer {
    val preview = FakeHomePreviewRepository()
    return createRuntimeContainer(context, preview, preview, preview)
}
