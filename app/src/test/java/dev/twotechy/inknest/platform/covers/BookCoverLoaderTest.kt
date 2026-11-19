package dev.twotechy.inknest.platform.covers

import androidx.compose.ui.graphics.ImageBitmap
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookCoverLoaderTest {
    private val request =
        BookCoverRequest("file:///covers/A.webp", BookCoverSize.forBounds(120, 180))
    private val image = Proxy.newProxyInstance(
        ImageBitmap::class.java.classLoader,
        arrayOf(ImageBitmap::class.java),
    ) { _, _, _ -> error("Cache lookup must not inspect pixels") } as ImageBitmap

    @Test fun warmCoverIsImmediateAndKeysIncludeThumbnailUriAndPixelSize() = runTest {
        var reads = 0
        val loader = BookCoverLoader(
            BookCoverDecoder {
                reads++
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
            maxBytes = 4 * 1024 * 1024,
            parentScope = backgroundScope,
        )
        assertNull(loader.peekCover(request))
        assertSame(image, loader.loadCover(request))
        assertSame(image, loader.peekCover(request))
        val warm = async(start = CoroutineStart.UNDISPATCHED) { loader.loadCover(request) }
        assertTrue(warm.isCompleted)
        assertSame(image, warm.await())
        assertEquals(1, reads)
        assertNull(loader.peekCover(request.copy(uri = "file:///covers/B.webp")))
        assertNull(loader.peekCover(request.copy(size = BookCoverSize.forBounds(240, 360))))
        assertSame(image, loader.peekCover(request.copy(size = BookCoverSize.forBounds(119, 179))))
    }

    @Test fun cacheEvictsLeastRecentlyUsedImagesWithinInjectedBudget() = runTest {
        val loader = BookCoverLoader(
            BookCoverDecoder { DecodedBookCover(image, 2 * 1024 * 1024) },
            StandardTestDispatcher(testScheduler),
            maxBytes = 4 * 1024 * 1024,
            parentScope = backgroundScope,
        )
        val second = request.copy(uri = "file:///covers/B.webp")
        val third = request.copy(uri = "file:///covers/C.webp")
        loader.loadCover(request)
        loader.loadCover(second)
        assertSame(image, loader.peekCover(request))
        loader.loadCover(third)
        assertNull(loader.peekCover(second))
        assertSame(image, loader.peekCover(request))
        assertSame(image, loader.peekCover(third))
        loader.clear()
        assertNull(loader.peekCover(request))
        assertNull(loader.peekCover(third))
    }

    @Test fun simultaneousRequestsForTheSameThumbnailOnlyDecodeOnce() = runTest {
        var reads = 0
        val release = CompletableDeferred<Unit>()
        val loader = BookCoverLoader(
            BookCoverDecoder {
                reads++
                release.await()
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
            parentScope = backgroundScope,
        )
        val first = async { loader.loadCover(request) }
        val second = async { loader.loadCover(request) }
        val third = async { loader.loadCover(request) }
        runCurrent()
        assertEquals(1, reads)
        first.cancelAndJoin()
        release.complete(Unit)
        assertSame(image, second.await())
        assertSame(image, third.await())
        assertSame(image, loader.peekCover(request))
        assertEquals(1, reads)
    }

    @Test fun clearingWhileDecodeIsPendingDoesNotRepopulateOldCache() = runTest {
        var reads = 0
        val old = CompletableDeferred<Unit>()
        val fresh = CompletableDeferred<Unit>()
        val loader = BookCoverLoader(
            BookCoverDecoder {
                if (++reads == 1) old.await() else fresh.await()
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
            parentScope = backgroundScope,
        )
        val first = async { loader.loadCover(request) }
        runCurrent()
        loader.clear()
        val second = async { loader.loadCover(request) }
        runCurrent()
        old.complete(Unit)
        assertNull(first.await())
        assertNull(loader.peekCover(request))
        val third = async { loader.loadCover(request) }
        runCurrent()
        assertEquals(2, reads)
        fresh.complete(Unit)
        assertSame(image, second.await())
        assertSame(image, third.await())
    }

    @Test fun decodePlanFitsDisplayBoundsWithoutUpscalingOrDecodingTheFullCover() {
        val target = BookCoverSize.forBounds(120, 180)
        assertEquals(BookCoverSize(128, 192), target)
        val plan = target.decodePlan(BookCoverSize(320, 480))
        assertEquals(BookCoverSize(128, 192), plan.output)
        assertEquals(2, plan.sample)
        val small = target.decodePlan(BookCoverSize(40, 60))
        assertEquals(BookCoverSize(40, 60), small.output)
        assertEquals(1, small.sample)
        assertEquals(BookCoverSize(480, 480), BookCoverSize.forBounds(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun differentKeysDecodeAtMostTwoAtOnceAndColorHasItsOwnEntry() = runTest {
        var active = 0
        var maximum = 0
        var reads = 0
        val release = CompletableDeferred<Unit>()
        val loader = BookCoverLoader(
            BookCoverDecoder {
                active++
                maximum = maxOf(maximum, active)
                reads++
                release.await()
                active--
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
            parentScope = backgroundScope,
        )
        val requests =
            listOf(request, request.copy(color = false), request.copy(uri = "file:///B.webp"))
        val pending = requests.map { async { loader.loadCover(it) } }
        runCurrent()
        assertEquals(2, reads)
        assertEquals(2, maximum)
        release.complete(Unit)
        pending.forEach { assertSame(image, it.await()) }
        assertEquals(3, reads)
        assertEquals(2, maximum)
        requests.forEach { assertSame(image, loader.peekCover(it)) }
    }
}
