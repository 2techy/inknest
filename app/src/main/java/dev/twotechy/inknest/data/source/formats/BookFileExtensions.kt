package dev.twotechy.inknest.data.source.formats

internal object BookFileExtensions {
    const val EPUB = "epub"
    const val PDF = "pdf"
    const val DJVU = "djvu"
    const val DJV = "djv"
    const val MOBI = "mobi"
    const val AZW = "azw"
    const val AZW3 = "azw3"
    const val FB2 = "fb2"
    const val TXT = "txt"
    const val CBZ = "cbz"
    const val CBR = "cbr"
    const val RTF = "rtf"
    const val DOC = "doc"
    const val DOCX = "docx"
    const val HTML = "html"
    const val HTM = "htm"
    const val CHM = "chm"
    const val PDB = "pdb"

    val recognized = setOf(
        EPUB, PDF, DJVU, DJV, MOBI, AZW, AZW3, FB2, TXT, CBZ, CBR,
        RTF, DOC, DOCX, HTML, HTM, CHM, PDB,
    )
    val metadata = setOf(EPUB, PDF, FB2, CBZ, MOBI, AZW3)
    const val JPG = "jpg"
    const val JPEG = "jpeg"
    const val PNG = "png"
    const val WEBP = "webp"
    val coverImages = setOf(JPG, JPEG, PNG, WEBP)

    fun fromName(name: String): String = name.substringAfterLast('.', "").lowercase()
}
