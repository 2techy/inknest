package dev.twotechy.inknest.data.source

import java.io.IOException
import java.io.InputStream

internal class MissingSourceFile : IOException()

internal interface SourceFileReader {
    suspend fun <T> read(tree: String, path: List<String>, parse: suspend (InputStream) -> T): T
}
