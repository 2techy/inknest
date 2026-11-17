package dev.twotechy.inknest.data.source.formats

// Android document providers often report octet-stream or aliases absent from reader intent filters.
internal object BookMimeTypes {
    private val types = mapOf(
        BookFileExtensions.EPUB to "application/epub+zip",
        BookFileExtensions.PDF to "application/pdf",
        BookFileExtensions.FB2 to "application/fb2",
        BookFileExtensions.CBZ to "application/vnd.comicbook+zip",
        BookFileExtensions.CBR to "application/vnd.comicbook-rar",
        BookFileExtensions.MOBI to "application/x-mobipocket-ebook",
        BookFileExtensions.AZW to "application/vnd.amazon.mobi8-ebook",
        BookFileExtensions.DJVU to "image/vnd.djvu",
        BookFileExtensions.DJV to "image/vnd.djvu",
        BookFileExtensions.TXT to "text/plain",
        BookFileExtensions.RTF to "application/rtf",
        BookFileExtensions.DOC to "application/msword",
        BookFileExtensions.DOCX to
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        BookFileExtensions.HTML to "text/html",
        BookFileExtensions.HTM to "text/html",
        BookFileExtensions.CHM to "application/vnd.ms-htmlhelp",
        BookFileExtensions.PDB to "application/vnd.palm",
    )

    fun forFile(name: String, reported: String?): String =
        types[BookFileExtensions.fromName(name)] ?: reported ?: "application/octet-stream"
}
