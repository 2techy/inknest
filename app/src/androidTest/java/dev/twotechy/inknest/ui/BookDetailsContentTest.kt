package dev.twotechy.inknest.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.BookReadingDay
import dev.twotechy.inknest.model.BookReadingStatistics
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.ui.library.BookDetailsActions
import dev.twotechy.inknest.ui.library.BookDetailsHero
import dev.twotechy.inknest.ui.library.BookDetailsScreen
import dev.twotechy.inknest.ui.library.BookDetailsUiState
import dev.twotechy.inknest.ui.library.LibraryGridBook
import dev.twotechy.inknest.ui.library.LibraryListBook
import dev.twotechy.inknest.ui.theme.InkNestTheme
import dev.twotechy.inknest.ui.theme.InkSpacing
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookDetailsContentTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var activity: MainActivity
    private val book = ReadingItem(
        ReadingItemId("test", "primary:Books/sample.epub"),
        "A long book title that wraps across several lines without losing its full name",
        "Sample author",
        37,
        status = ReadingStatus.Reading,
        currentPage = 132,
        totalPages = 356,
    )

    @Before fun launch() {
        runBlocking { (context.applicationContext as InkNestApplication).awaitContainer() }
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.waitForIdleSync()
    }

    @After fun close() = instrumentation.runOnMainSync { activity.finish() }

    @Test fun longTitlesAndReadingActionRemainAccessibleAtLargeFont() {
        for ((width, font) in listOf(320f to 1f, 320f to 2f, 720f to 1f)) {
            val opened = AtomicInteger()
            show(width, font) {
                BookDetailsHero(book, ZoneId.of("UTC")) { opened.incrementAndGet() }
            }
            awaitText(book.title, true)
            click(context.getString(R.string.home_continue))
            assertEquals(1, opened.get())
        }
    }

    @Test fun lastReadReturnsBelowProgressWithFullWidthReadingAction() {
        val time = Instant.parse("2026-11-16T08:24:00Z")
        val sample = book.copy(title = "Sample book", lastReadAt = time.toEpochMilli())
        val opened = AtomicInteger()
        val zone = ZoneId.of("UTC")
        val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(context.resources.configuration.locales[0]).format(time.atZone(zone))
        val lastRead = context.getString(R.string.book_details_last_read_value, date)
        show(412f, 1f) { BookDetailsHero(sample, zone) { opened.incrementAndGet() } }
        awaitText(lastRead, true)
        val action = actionBounds(context.getString(R.string.home_continue))
        val title = Rect().also { rect ->
            nodes().first { it.text?.toString() == sample.title }.getBoundsInScreen(rect)
        }
        assertTrue(action.left < title.left)
        click(context.getString(R.string.home_continue))
        assertEquals(1, opened.get())
        show(412f, 1f) { BookDetailsHero(book, zone) {} }
        awaitText(lastRead, false)
    }

    private fun actionBounds(text: String): Rect {
        var node = nodes().first { it.text?.toString() == text }
        while (!node.isClickable) node = checkNotNull(node.parent)
        return Rect().also(node::getBoundsInScreen)
    }

    @Test fun shelfCaptionsOmitStatusAndFinishedReplacesAnyPercentage() {
        val opened = AtomicInteger()
        for (progress in listOf(null, 42, 100)) {
            val finished = book.copy(progress = progress, status = ReadingStatus.Finished)
            show(400f, 1f) {
                LibraryListBook(finished, { opened.incrementAndGet() }, {})
            }
            awaitText(context.getString(R.string.library_filter_finished), true)
            assertTrue(nodes().none { it.text?.toString() == "$progress%" })
            click(book.title)
        }
        assertEquals(3, opened.get())
        show(400f, 1f) {
            LibraryGridBook(book.copy(status = ReadingStatus.Unknown, progress = 100), {}, {})
        }
        awaitText("100%", true)
        awaitText(context.getString(R.string.library_filter_finished), false)
    }

    @Test fun onePageExpandsDescriptionHistoryAndFileInPlace() {
        val state = mutableStateOf(
            BookDetailsUiState(
                details = BookDetails(
                    book = book.copy(
                        title = "Sample book",
                        lastReadAt = Instant.parse("2026-11-16T08:24:00Z").toEpochMilli(),
                        description = "A sample description. ".repeat(40),
                    ),
                    loading = false,
                    statistics = BookReadingStatistics(
                        status = BookStatisticsStatus.Ready,
                        seconds = 16320,
                        pages = 186,
                        readingDays = 8,
                    ),
                ),
            ),
        )
        show(400f, 1f) {
            BookDetailsScreen(
                state.value,
                BookDetailsActions(
                    {},
                    {},
                    {
                        state.value =
                            state.value.copy(descriptionExpanded = !state.value.descriptionExpanded)
                    },
                    {
                        state.value = state.value.copy(
                            historyExpanded = !state.value.historyExpanded,
                        )
                    },
                    {
                        state.value = state.value.copy(fileExpanded = !state.value.fileExpanded)
                    },
                    {},
                    {},
                ),
                contentPadding = PaddingValues(InkSpacing.lg),
            )
        }
        awaitText(context.getString(R.string.book_details_statistics), true)
        assertTrue(nodes().none { it.text?.toString() == "8 days" })
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "book-details-default.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
        click(context.getString(R.string.book_details_history))
        assertTrue(state.value.historyExpanded)
        click(context.getString(R.string.book_details_file))
        assertTrue(state.value.fileExpanded)
        scrollToText("Books/sample.epub")
        awaitText("Books/sample.epub", true)
        scrollToText(context.getString(R.string.book_details_show_more), backwards = true)
        click(context.getString(R.string.book_details_show_more))
        assertTrue(state.value.descriptionExpanded)
    }

    @Test fun historyPagesReplaceRowsAndOmitHistoryDateSummaries() {
        val days = (0..11).map {
            BookReadingDay(LocalDate.of(2026, 11, 16).minusDays(it.toLong()), 60, 1)
        }
        val statistics = BookReadingStatistics(
            status = BookStatisticsStatus.Ready,
            readingDays = days.size,
            firstRead = days.last().date,
            lastRead = days.first().date,
            history = days.take(5),
            hasMore = true,
        )
        val state = mutableStateOf(
            BookDetailsUiState(
                details = BookDetails(
                    book.copy(title = "Sample"),
                    false,
                    statistics,
                ),
                historyExpanded = true,
            ),
        )
        val actions = BookDetailsActions(
            back = {},
            open = {},
            description = {},
            history = {},
            file = {},
            page = { page ->
                val updated = statistics.copy(
                    historyPage = page,
                    history = days.drop(page * 5).take(5),
                    hasMore = page < 2,
                )
                val details = state.value.details.copy(statistics = updated)
                state.value = state.value.copy(details = details)
            },
            retry = {},
        )
        show(412f, 1f) {
            BookDetailsScreen(state.value, actions, contentPadding = PaddingValues(InkSpacing.lg))
        }
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(context.resources.configuration.locales[0])
        val next = context.getString(R.string.book_details_next_page)
        scrollToText(next)
        click(next)
        awaitText(context.getString(R.string.book_details_history_page, 2, 3), true)
        awaitText(date.format(days.first().date), false)
        awaitText(date.format(days[5].date), true)
        click(context.getString(R.string.book_details_previous_page))
        awaitText(context.getString(R.string.book_details_history_page, 1, 3), true)
        assertTrue(
            nodes().none {
                val text = it.text?.toString().orEmpty()
                text.startsWith("First read:") || text.startsWith("Last read:")
            },
        )
    }

    @Test fun unmatchedStatisticsHaveNoRetry() {
        val state = BookDetailsUiState(
            details = BookDetails(
                book = book,
                statistics = BookReadingStatistics(status = BookStatisticsStatus.Unlinked),
            ),
        )
        show(412f, 1f) {
            BookDetailsScreen(state, BookDetailsActions({}, {}, {}, {}, {}, {}, {}))
        }
        awaitText(context.getString(R.string.book_details_stats_unlinked), true)
        awaitText(context.getString(R.string.book_details_retry), false)
    }

    private fun show(
        width: Float,
        font: Float,
        preferences: AppPreferences = AppPreferences(),
        content: @Composable () -> Unit,
    ) {
        val composed = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val density = activity.resources.displayMetrics.widthPixels / width
            activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(density, font)) {
                    InkNestTheme(preferences) {
                        key(width, font) {
                            Column(
                                Modifier.fillMaxWidth().windowInsetsPadding(
                                    WindowInsets.safeDrawing,
                                ),
                            ) {
                                content()
                            }
                        }
                        SideEffect { composed.countDown() }
                    }
                }
            }
        }
        assertTrue("fixture composed", composed.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(200, 5_000)
        val frame = CountDownLatch(1)
        instrumentation.runOnMainSync {
            activity.window.decorView.postOnAnimation {
                activity.window.decorView.postOnAnimation { frame.countDown() }
            }
        }
        assertTrue("frame rendered", frame.await(5, TimeUnit.SECONDS))
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val root = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val result = mutableListOf<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.refresh()
            result.add(node)
            repeat(node.childCount) { index -> node.getChild(index)?.let(queue::add) }
        }
        return result
    }

    private fun awaitText(text: String, present: Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (hasText(text) != present && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
        }
        assertEquals(text, present, hasText(text))
    }

    private fun hasText(text: String) = nodes().any {
        it.text?.toString()?.contains(text) == true ||
            it.contentDescription?.toString()?.contains(text) == true
    }

    private fun scrollToText(text: String, backwards: Boolean = false) {
        repeat(8) {
            if (hasText(text)) return
            val action = if (backwards) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            nodes().firstOrNull { it.isScrollable }?.performAction(action)
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.waitForIdle(100, 5_000)
        }
    }

    private fun click(text: String) {
        val candidates = nodes()
        var node = candidates.firstOrNull {
            it.text?.toString() == text || it.contentDescription?.toString() == text
        } ?: candidates.first {
            it.text?.toString()?.contains(text) == true ||
                it.contentDescription?.toString()?.contains(text) == true
        }
        while (!node.isClickable) node = checkNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(100, 5_000)
    }
}
