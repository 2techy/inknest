package dev.twotechy.inknest.data.source.formats

import dev.twotechy.inknest.data.source.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.UnsupportedBookFormat
import dev.twotechy.inknest.data.source.formats.BookFileExtensions
import dev.twotechy.inknest.data.source.formats.cbz.CbzReader
import dev.twotechy.inknest.data.source.formats.epub.EpubReader
import dev.twotechy.inknest.data.source.formats.fb2.Fb2Reader
import dev.twotechy.inknest.data.source.formats.mobi.MobiReader
import dev.twotechy.inknest.data.source.formats.pdf.PdfReader
import java.io.File

internal class BookReader {
    suspend fun read(
        file: File,
        extension: String,
        cover: suspend (ByteArray) -> Unit,
    ): BookMetadataProbeResult = when (extension) {
        BookFileExtensions.EPUB -> EpubReader().read(
            file,
            cover,
        )

        BookFileExtensions.FB2 -> Fb2Reader().read(file, cover)

        BookFileExtensions.CBZ -> CbzReader().read(file, cover)

        BookFileExtensions.MOBI, BookFileExtensions.AZW3 -> MobiReader().read(file, cover)

        BookFileExtensions.PDF -> PdfReader().read(
            file,
            cover,
        )

        else -> throw UnsupportedBookFormat()
    }
}
