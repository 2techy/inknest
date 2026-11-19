package dev.twotechy.inknest.data.source.saf

import java.io.IOException

internal class MissingSourceFileException(cause: Throwable? = null) :
    IOException("Source file is unavailable", cause)
