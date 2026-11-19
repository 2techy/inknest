package dev.twotechy.inknest.data.source.statistics

internal data class SnapshotFingerprint(
    val database: SnapshotFileMetadata,
    val wal: SnapshotFileMetadata,
    val shm: SnapshotFileMetadata,
) {
    val hasWal: Boolean get() = wal != SnapshotFileMetadata.Absent

    fun reusableAt(now: Long): Boolean = database is SnapshotFileMetadata.Present &&
        listOf(database, wal, shm).all {
            when (it) {
                SnapshotFileMetadata.Absent -> true

                SnapshotFileMetadata.Unknown -> false

                is SnapshotFileMetadata.Present ->
                    it.modifiedAt > 0 && it.modifiedAt <= now - SETTLE_MILLIS
            }
        }

    private companion object {
        const val SETTLE_MILLIS = 5_000L
    }
}
