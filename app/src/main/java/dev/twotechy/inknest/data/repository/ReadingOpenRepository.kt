package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.UnresolvedBookPath
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal interface ReadingOpenRepository {
    suspend fun prepare(item: ReadingItemId): ReadingOpenPreparation
    suspend fun isCurrent(prepared: PreparedReadingOpen): Boolean
}

internal class SafReadingOpenRepository(
    private val config: ReadingSourceConfigRepository,
    private val books: BookProbeDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ReadingOpenRepository {
    override suspend fun prepare(item: ReadingItemId): ReadingOpenPreparation {
        val selected = config.config.first()
        if (selected.sourceId != item.sourceId) return failed(OpenResult.SourceChanged)
        if (selected.sourcePackage == null) return failed(OpenResult.MissingApp)
        return try {
            val prepared = resolve(selected, item)
            if (isCurrent(
                    prepared,
                )
            ) {
                ReadingOpenPreparation.Ready(prepared)
            } else {
                failed(OpenResult.SourceChanged)
            }
        } catch (_: MissingSourceFile) {
            failed(OpenResult.MissingFile)
        } catch (_: UnresolvedBookPath) {
            failed(OpenResult.PermissionLost)
        } catch (_: SecurityException) {
            failed(OpenResult.PermissionLost)
        } catch (_: IOException) {
            failed(OpenResult.Unavailable)
        }
    }
    private suspend fun resolve(selected: ReadingSourceConfig, item: ReadingItemId) =
        withContext(ioDispatcher) {
            val book = books.resolveIdentity(selected, item.path)
            val mime = if (book.relative.last().endsWith(".epub", true)) {
                "application/epub+zip"
            } else {
                book.document.mime ?: "application/octet-stream"
            }
            PreparedReadingOpen(selected, book.document.uri, mime)
        }

    override suspend fun isCurrent(prepared: PreparedReadingOpen): Boolean =
        config.config.first() == prepared.source

    private fun failed(error: OpenResult) = ReadingOpenPreparation.Failed(error)
}
