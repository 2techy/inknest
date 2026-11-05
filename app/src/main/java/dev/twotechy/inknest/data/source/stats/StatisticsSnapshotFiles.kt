package dev.twotechy.inknest.data.source.stats

import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.copyBounded
import java.io.File
import java.io.IOException

// A snapshot attempt is bound to one selected tree; filenames are a fixed local allowlist.
internal class StatisticsSnapshotFiles(
    private val files: SourceDocumentReader,
    private val tree: String,
) {
    suspend fun copyTo(directory: File): Boolean {
        val before = metadata()
        if (before[DATABASE_NAME] == null) throw MissingSourceFile()
        copyFiles(directory, before)
        if (before != metadata()) throw StatisticsSnapshotChanged()
        return before["$DATABASE_NAME-wal"] != null
    }

    private suspend fun metadata(): Map<String, SourceDocument?> =
        listOf(DATABASE_NAME, "$DATABASE_NAME-wal", "$DATABASE_NAME-shm").associateWith { name ->
            try {
                files.describe(tree, listOf("settings", name))
            } catch (_: MissingSourceFile) {
                null
            }
        }

    private suspend fun copyFiles(directory: File, metadata: Map<String, SourceDocument?>) {
        var remaining = 256L * 1024 * 1024
        for (name in metadata.filterValues { it != null }.keys) {
            try {
                files.read(tree, listOf("settings", name)) { input ->
                    File(directory, name).outputStream().use { copyBounded(input, it, remaining) }
                }
            } catch (_: MissingSourceFile) {
                throw StatisticsSnapshotChanged()
            }
            remaining -= File(directory, name).length()
        }
    }

    companion object {
        const val DATABASE_NAME = "statistics.sqlite3"
    }
}

internal class StatisticsSnapshotChanged : IOException()
