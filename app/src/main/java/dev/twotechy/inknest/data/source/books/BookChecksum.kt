package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.fingerprint

internal data class BookChecksum(val tree: String, val fingerprint: String?, val checksum: String)
