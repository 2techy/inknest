package dev.twotechy.inknest.ui.settings

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

internal enum class ReleaseNoteStyle { Body, Heading, Quote, Code, Rule }

internal data class ReleaseNoteBlock(
    val text: AnnotatedString,
    val style: ReleaseNoteStyle = ReleaseNoteStyle.Body,
    val depth: Int = 0,
    val marker: String? = null,
)

internal data class ReleaseNotesDocument(
    val source: String = "",
    val blocks: List<ReleaseNoteBlock> = emptyList(),
)

internal class ReleaseNotesParser(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun parse(source: String, baseUrl: String): ReleaseNotesDocument =
        withContext(dispatcher) {
            val root = Parser.builder().build().parse(source.take(100_000))
            val visitor = ReleaseNotesVisitor(baseUrl, currentCoroutineContext().job)
            root.accept(visitor)
            ReleaseNotesDocument(source, visitor.blocks.toList())
        }
}

private class ReleaseNotesVisitor(private val baseUrl: String, private val job: Job) :
    AbstractVisitor() {
    val blocks = mutableListOf<ReleaseNoteBlock>()
    private var listDepth = 0
    private var quoteDepth = 0
    private var marker: String? = null

    override fun visit(paragraph: Paragraph) {
        val style = if (quoteDepth > 0) ReleaseNoteStyle.Quote else ReleaseNoteStyle.Body
        emit(ReleaseNotesInline(baseUrl).render(paragraph), style)
    }

    override fun visit(heading: Heading) {
        emit(ReleaseNotesInline(baseUrl).render(heading), ReleaseNoteStyle.Heading)
    }

    override fun visit(bulletList: BulletList) = visitList(bulletList) { "•" }

    override fun visit(orderedList: OrderedList) {
        var number = orderedList.markerStartNumber ?: 1
        visitList(orderedList) { "${number++}." }
    }

    override fun visit(blockQuote: BlockQuote) {
        if (quoteDepth >= 16) return
        quoteDepth++
        visitChildren(blockQuote)
        quoteDepth--
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) = code(fencedCodeBlock.literal)

    override fun visit(indentedCodeBlock: IndentedCodeBlock) = code(indentedCodeBlock.literal)

    override fun visit(thematicBreak: ThematicBreak) {
        emit(AnnotatedString(""), ReleaseNoteStyle.Rule)
    }

    override fun visit(htmlBlock: HtmlBlock) = Unit

    private fun visitList(list: Node, nextMarker: () -> String) {
        if (listDepth >= 16) return
        val outerMarker = marker
        listDepth++
        var item = list.firstChild
        while (item != null) {
            marker = nextMarker()
            item.accept(this)
            item = item.next
        }
        listDepth--
        marker = outerMarker
    }

    private fun code(literal: String) = emit(
        AnnotatedString(literal.trimEnd('\n')),
        ReleaseNoteStyle.Code,
    )

    private fun emit(text: AnnotatedString, style: ReleaseNoteStyle) {
        job.ensureActive()
        if (blocks.size >= 1_000) return
        blocks += ReleaseNoteBlock(text, style, (listDepth - 1).coerceAtLeast(0), marker)
        marker = null
    }
}
