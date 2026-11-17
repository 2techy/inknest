package dev.twotechy.inknest.data.source

import java.io.File

internal interface UpdateHttpTransport {
    suspend fun text(url: String, limit: Int): String
    suspend fun download(url: String, target: File, size: Long, progress: (Int) -> Unit)
}
