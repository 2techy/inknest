package dev.twotechy.inknest.data.source

import java.io.FileNotFoundException
import java.io.IOException

// Only wrap resolver calls, not URI validation or the parser consuming a returned stream.
internal fun <T> safProviderOperation(read: () -> T): T = try {
    read()
} catch (missing: FileNotFoundException) {
    throw MissingSourceFile(missing)
} catch (failure: IllegalArgumentException) {
    // Some ExternalStorageProvider versions wrap a disappearing document in this exception.
    throw IOException("Storage provider could not access the document", failure)
}
