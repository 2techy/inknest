package dev.twotechy.inknest

import android.app.Application
import dev.twotechy.inknest.di.createAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InkNestApplication : Application() {
    internal val container by lazy { createAppContainer(this) }

    internal suspend fun awaitContainer() = withContext(Dispatchers.Default) { container }

    override fun onCreate() {
        super.onCreate()
        configureRuntimeDiagnostics()
    }
}
