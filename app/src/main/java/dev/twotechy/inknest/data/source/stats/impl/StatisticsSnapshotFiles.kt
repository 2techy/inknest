package dev.twotechy.inknest.data.source.stats.impl

import dev.twotechy.inknest.data.source.io.copyBounded
import dev.twotechy.inknest.data.source.saf.MissingSourceFile
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.data.source.stats.SnapshotFileMetadata
import dev.twotechy.inknest.data.source.stats.SnapshotFingerprint
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotChanged
import java.io.File

// A snapshot attempt is bound to one selected tree; filenames are a fixed local allowlist.
internal class StatisticsSnapshotFiles(
    private val files: SourceDocumentReader,
    private val tree: String,
) {
    suspend fun fingerprint(): SnapshotFingerprint = metadata().fingerprint()

    suspend fun copyTo(directory: File): SnapshotFingerprint {
        val before = metadata()
        if (before[DATABASE_NAME] == null) throw MissingSourceFile()
        copyFiles(directory, before)
        if (before != metadata()) throw StatisticsSnapshotChanged()
        return before.fingerprint()
    }

    private fun Map<String, SourceDocument?>.fingerprint() = SnapshotFingerprint(
        get(DATABASE_NAME).fingerprint(),
        get("$DATABASE_NAME-wal").fingerprint(),
        get("$DATABASE_NAME-shm").fingerprint(),
    )

    private fun SourceDocument?.fingerprint(): SnapshotFileMetadata {
        if (this == null) return SnapshotFileMetadata.Absent
        val bytes = size
        val timestamp = modified
        if (bytes == null || timestamp == null) return SnapshotFileMetadata.Unknown
        if (bytes < 0 || timestamp <= 0) return SnapshotFileMetadata.Unknown
        return SnapshotFileMetadata.Present(bytes, timestamp)
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
