package dev.twotechy.inknest.data.source.formats

import dev.twotechy.inknest.data.source.formats.BookFileExtensions
import dev.twotechy.inknest.data.source.formats.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.formats.UnsupportedBookFormatException
import dev.twotechy.inknest.data.source.formats.cbz.CbzParser
import dev.twotechy.inknest.data.source.formats.epub.EpubParser
import dev.twotechy.inknest.data.source.formats.fb2.Fb2Parser
import dev.twotechy.inknest.data.source.formats.mobi.MobiParser
import dev.twotechy.inknest.data.source.formats.pdf.PdfParser
import java.io.File

internal class BookFormatParser {
    suspend fun read(
        file: File,
        extension: String,
        cover: suspend (ByteArray) -> Unit,
    ): BookMetadataProbeResult = when (extension) {
        BookFileExtensions.EPUB -> EpubParser().read(
            file,
            cover,
        )

        BookFileExtensions.FB2 -> Fb2Parser().read(file, cover)

        BookFileExtensions.CBZ -> CbzParser().read(file, cover)

        BookFileExtensions.MOBI, BookFileExtensions.AZW3 -> MobiParser().read(file, cover)

        BookFileExtensions.PDF -> PdfParser().read(
            file,
            cover,
        )

        else -> throw UnsupportedBookFormatException()
    }
}
