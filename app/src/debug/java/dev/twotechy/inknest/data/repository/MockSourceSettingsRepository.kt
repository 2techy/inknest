package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.preview.SourcePreviewController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class MockSourceSettingsRepository :
    SourceSettingsRepository,
    SourcePreviewController {
    private val current = MutableStateFlow(SourceSettingsState(available = true, isSample = true))
    override val state = current.asStateFlow()
    private var failNext = false
    override fun failNextAction() {
        failNext = true
    }
    private fun checkFailure() {
        if (failNext) {
            failNext = false
            error("Simulated source failure")
        }
    }
    override suspend fun connect(folder: SourceFolder) {
        checkFailure()
        current.update { old ->
            val koreader = if (folder ==
                SourceFolder.KOReader
            ) {
                "Sample/KOReader"
            } else {
                old.koreaderFolder
            }
            val books = if (folder == SourceFolder.Books) "Sample/Books" else old.booksFolder
            old.copy(
                koreaderFolder = koreader,
                booksFolder = books,
                status = if (koreader !=
                    null
                ) {
                    SourceStatus.Connected
                } else {
                    SourceStatus.NotConnected
                },
            )
        }
    }
    override suspend fun disconnect() {
        checkFailure()
        current.value = SourceSettingsState(available = true, isSample = true)
    }
    override suspend fun refresh() {
        check(state.value.status == SourceStatus.Connected)
        current.update { it.copy(refreshing = true) }
        try {
            delay(500)
            checkFailure()
            current.update {
                it.copy(lastUpdated = System.currentTimeMillis(), cacheBytes = 32768L)
            }
        } finally {
            current.update { it.copy(refreshing = false) }
        }
    }
    override suspend fun clearCache() {
        checkFailure()
        current.update { it.copy(cacheBytes = 0, lastUpdated = null) }
    }
    override fun selectStatus(status: SourceStatus) {
        current.value =
            if (status ==
                SourceStatus.NotConnected
            ) {
                SourceSettingsState(available = true, isSample = true)
            } else {
                current.value.copy(
                    status = status,
                    koreaderFolder = "Sample/KOReader",
                    booksFolder = "Sample/Books",
                )
            }
    }
}
