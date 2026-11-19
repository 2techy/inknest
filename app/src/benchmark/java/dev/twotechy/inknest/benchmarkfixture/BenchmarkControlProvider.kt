package dev.twotechy.inknest.benchmarkfixture

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.platform.AppOperationTrace
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

// Shell-only control remains outside production manifests and source sets.
class BenchmarkControlProvider : ContentProvider() {
    @Volatile private var phase = "idle"
    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    private fun snapshot(): Bundle {
        val refresh = AppOperationTrace.tracer.snapshot(BenchmarkFixture.REFRESH_TRACE)
        val scan = AppOperationTrace.tracer.snapshot(BenchmarkFixture.SCAN_TRACE)
        return Bundle().apply {
            putInt("refreshes", refresh.started)
            putInt("scans", scan.started)
            putInt("active", refresh.active + scan.active)
            putString("phase", phase)
        }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        checkNotNull(context).enforceCallingOrSelfPermission(Manifest.permission.DUMP, "Shell only")
        if (method == "state") return snapshot()
        return try {
            runBlocking {
                withTimeout(BenchmarkFixture.TIMEOUT_MILLIS) {
                    val app = checkNotNull(context).applicationContext as InkNestApplication
                    phase = "container"
                    val container = app.awaitContainer()
                    AppOperationTrace.tracer.begin("InkNest.control.$method").use {
                        execute(container, method)
                        validate(container, changed = arg == "changed")
                    }
                    phase = "complete"
                    Bundle().apply { putBoolean("valid", true) }
                }
            }
        } catch (error: TimeoutCancellationException) {
            throw IllegalStateException(
                "Benchmark validation timed out at $phase; state=$arg",
                error,
            )
        }
    }

    private suspend fun execute(container: AppContainer, method: String) {
        phase = method
        when (method) {
            "clear" -> clear(container)
            "prepare" -> prepare(container)
            "refresh-library" -> container.libraryRepository.refresh()
            "refresh-statistics" -> refreshStatistics(container)
            "validate" -> Unit
            else -> error("Unknown benchmark operation")
        }
    }

    private suspend fun prepare(container: AppContainer) {
        phase = "prepare-catalog"
        val catalog = container.libraryRepository.catalog.first {
            it.configured && !it.loading && !it.sync.running
        }
        if (catalog.books.size != BenchmarkFixture.BOOKS ||
            catalog.books.any { it.coverUri == null }
        ) {
            clear(container)
        }
    }

    private suspend fun refreshStatistics(container: AppContainer) {
        val before = container.statisticsRepository.data.value.sync.lastAttemptAt
        container.statisticsRepository.refresh()
        val result = container.statisticsRepository.data.first {
            !it.sync.running && (it.sync.lastAttemptAt != before || it.sync.error != null)
        }
        check(result.sync.error == null)
    }

    private suspend fun clear(container: AppContainer) {
        phase = "clear-cache"
        val previous = AppOperationTrace.tracer.snapshot(BenchmarkFixture.SCAN_TRACE)
        container.sourceSettingsRepository.clearCache()
        while (true) {
            val scan = AppOperationTrace.tracer.snapshot(BenchmarkFixture.SCAN_TRACE)
            if (scan.started > previous.started && scan.active == 0) return
            delay(100)
        }
    }

    private suspend fun validate(container: AppContainer, changed: Boolean) {
        val progress = if (changed) 38 else 37
        val seconds = (0 until BenchmarkFixture.EVENTS).sumOf { (15 + it % 75).toLong() } +
            if (changed) 77 else 0
        phase = "reading"
        container.readingRepository.reading.first {
            it is ReadingState.Content && it.current.progress == progress
        }
        phase = "history"
        container.readingRepository.trackedItems.first { it.size == BenchmarkFixture.HISTORY }
        phase = "statistics"
        container.statisticsRepository.data.first {
            it.updatedAt != null && it.days.values.sumOf { day -> day.seconds } == seconds
        }
        phase = "catalog"
        val catalog = container.libraryRepository.catalog.first {
            it.books.size == BenchmarkFixture.BOOKS && !it.loading && !it.sync.running &&
                it.sync.error == null
        }
        check(catalog.books.all { it.coverUri != null })
        check(container.readingRepository.sync.first().error == null)
        check(container.statisticsRepository.data.value.sync.error == null)
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/inknest-benchmark"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read-only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Read-only")
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = error("Read-only")
}
