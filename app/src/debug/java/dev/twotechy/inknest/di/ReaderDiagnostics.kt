package dev.twotechy.inknest.di

import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import java.io.File

internal fun createReaderDiagnostics(
    files: SourceDocumentReader,
    books: BookProbeDataSource,
    cache: File,
) = ReaderProbeRepository(files, books = books, stats = StatisticsSnapshotProbe(files, cache))
