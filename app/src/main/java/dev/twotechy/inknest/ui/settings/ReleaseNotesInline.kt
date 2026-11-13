package dev.twotechy.inknest.ui.settings

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.net.URI
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text

internal class ReleaseNotesInline(private val baseUrl: String) : AbstractVisitor() {
    private val text = AnnotatedString.Builder()

    fun render(node: Node): AnnotatedString {
        visitChildren(node)
        return text.toAnnotatedString()
    }

    override fun visit(node: Text) {
        text.append(node.literal)
    }

    override fun visit(node: SoftLineBreak) {
        text.append(" ")
    }

    override fun visit(node: HardLineBreak) {
        text.append("\n")
    }

    override fun visit(node: StrongEmphasis) = styled(node, SpanStyle(fontWeight = FontWeight.Bold))

    override fun visit(node: Emphasis) = styled(node, SpanStyle(fontStyle = FontStyle.Italic))

    override fun visit(node: Code) {
        text.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
        text.append(node.literal)
        text.pop()
    }

    override fun visit(node: Link) {
        val url = releaseNoteUrl(node.destination, baseUrl)
        if (url == null) {
            visitChildren(node)
            return
        }
        text.pushLink(
            LinkAnnotation.Url(
                url,
                TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
            ),
        )
        visitChildren(node)
        text.pop()
    }

    // Keep image descriptions readable without fetching external images.
    override fun visit(node: Image) = visitChildren(node)

    override fun visit(node: HtmlInline) = Unit

    private fun styled(node: Node, style: SpanStyle) {
        text.pushStyle(style)
        visitChildren(node)
        text.pop()
    }
}

internal fun releaseNoteUrl(destination: String, baseUrl: String): String? = try {
    val resolved = URI(baseUrl).resolve(destination)
    resolved.takeIf { it.scheme in setOf("https", "http") && it.host != null }?.toASCIIString()
} catch (_: IllegalArgumentException) {
    null
} catch (_: java.net.URISyntaxException) {
    null
}

internal fun bindReleaseNoteLinks(
    text: AnnotatedString,
    openLink: (String) -> Unit,
): AnnotatedString = text.mapAnnotations { range ->
    val link = range.item as? LinkAnnotation.Url
    if (link == null) {
        range
    } else {
        AnnotatedString.Range(
            LinkAnnotation.Clickable(link.url, link.styles) { openLink(link.url) },
            range.start,
            range.end,
            range.tag,
        )
    }
}
