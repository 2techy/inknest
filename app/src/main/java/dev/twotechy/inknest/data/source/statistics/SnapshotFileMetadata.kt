package dev.twotechy.inknest.data.source.statistics

internal sealed interface SnapshotFileMetadata {
    data class Present(val size: Long, val modifiedAt: Long) : SnapshotFileMetadata
    data object Absent : SnapshotFileMetadata
    data object Unknown : SnapshotFileMetadata
}
