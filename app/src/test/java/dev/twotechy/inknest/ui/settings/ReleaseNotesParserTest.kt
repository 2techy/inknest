package dev.twotechy.inknest.ui.settings

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesParserTest {
    @Test fun rendersHeadingsEmphasisCodeAndNestedListsWithoutMarkdownPunctuation() = runTest {
        val source = """
            ## Hello **reader**

            Read *more* with `InkNest`.

            - First
              - Nested

            3. Third
            4. Fourth

            > A quote

            ---

            ```kotlin
            val text = "**literal**"
            ```
        """.trimIndent()
        val document = ReleaseNotesParser(StandardTestDispatcher(testScheduler)).parse(source, BASE)
        assertEquals(source, document.source)
        val title = document.blocks.first()
        assertEquals(ReleaseNoteStyle.Heading, title.style)
        assertEquals("Hello reader", title.text.text)
        assertTrue(title.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        val paragraph = document.blocks[1].text
        assertEquals("Read more with InkNest.", paragraph.text)
        assertTrue(paragraph.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(paragraph.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
        val list = document.blocks.filter { it.marker != null }
        assertEquals(listOf("•", "•", "3.", "4."), list.map { it.marker })
        assertEquals(listOf(0, 1, 0, 0), list.map { it.depth })
        assertTrue(document.blocks.any { it.style == ReleaseNoteStyle.Quote })
        assertTrue(document.blocks.any { it.style == ReleaseNoteStyle.Rule })
        val code = document.blocks.single { it.style == ReleaseNoteStyle.Code }
        assertEquals("val text = \"**literal**\"", code.text.text)
    }

    @Test fun linksAreClickableOnlyForWebUrlsAndHtmlOrImagesDoNotExecute() = runTest {
        val source = """
            [Help](https://example.com/help) [Guide](../guide) [Bad](javascript:alert(1))
            ![Cover](https://example.com/cover.png)

            <!-- hidden -->
            <script>hidden()</script>
        """.trimIndent()
        val blocks = ReleaseNotesParser(
            StandardTestDispatcher(testScheduler),
        ).parse(source, BASE).blocks
        val text = blocks.first().text
        assertEquals("Help Guide Bad Cover", text.text)
        val links = text.getLinkAnnotations(0, text.length).map {
            (it.item as LinkAnnotation.Url).url
        }
        assertEquals(listOf("https://example.com/help", "https://example.com/guide"), links)
        assertFalse(blocks.any { it.text.text.contains("hidden") })
        assertNull(releaseNoteUrl("file:///private", BASE))
        assertNull(releaseNoteUrl("intent://install", BASE))
        assertNull(releaseNoteUrl("javascript:alert(1)", BASE))
        assertNull(releaseNoteUrl("https://invalid host/", BASE))
    }

    @Test fun linksDispatchThroughRouteWithoutMutatingParsedContent() = runTest {
        val document = ReleaseNotesParser(StandardTestDispatcher(testScheduler))
            .parse("[Help](https://example.com/help)", BASE)
        val original = document.blocks.single().text
        val opened = mutableListOf<String>()
        val rendered = bindReleaseNoteLinks(original, opened::add)
        val link = rendered.getLinkAnnotations(0, rendered.length).single().item
            as LinkAnnotation.Clickable
        link.linkInteractionListener!!.onClick(link)
        assertEquals(listOf("https://example.com/help"), opened)
        assertTrue(
            original.getLinkAnnotations(0, original.length).single().item is LinkAnnotation.Url,
        )
    }

    private companion object {
        const val BASE = "https://example.com/releases/v1"
    }
}
