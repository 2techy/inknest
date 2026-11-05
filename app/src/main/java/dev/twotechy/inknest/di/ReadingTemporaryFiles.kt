package dev.twotechy.inknest.di

import android.content.Context
import java.io.File

// Runs once, off main, before source jobs start. Only InkNest's interrupted temporary writes.
internal fun cleanReadingTemporaryFiles(context: Context) {
    val cache = context.cacheDir.canonicalFile
    cache.listFiles()?.filter {
        it.name.startsWith("reader-epub-") || it.name.startsWith("reader-stats-")
    }?.forEach { directory ->
        if (directory.canonicalFile.parentFile == cache) directory.deleteRecursively()
    }
    val covers = File(context.filesDir, "reading-covers").canonicalFile
    covers.listFiles()?.filter { it.name.startsWith("cover-") && it.extension == "tmp" }
        ?.forEach { file ->
            if (file.canonicalFile.parentFile == covers) file.delete()
        }
}
