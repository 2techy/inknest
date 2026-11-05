package dev.twotechy.inknest.platform

import androidx.compose.ui.graphics.ImageBitmap
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
        BookCoverRequest("file:///covers/A.png", BookCoverSize.forBounds(120, 180))
    private val image = Proxy.newProxyInstance(
        ImageBitmap::class.java.classLoader,
        arrayOf(ImageBitmap::class.java),
    ) { _, _, _ -> error("Cache lookup must not inspect pixels") } as ImageBitmap

    @Test fun warmCoverIsImmediateAndKeysIncludeThumbnailUriAndPixelSize() = runTest {
        var reads = 0
        val loader = BookCoverLoader(
            BookCoverSource {
                reads++
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
        )
        assertNull(loader.peekCover(request))
        assertSame(image, loader.loadCover(request))
        assertSame(image, loader.peekCover(request))
        val warm = async(start = CoroutineStart.UNDISPATCHED) { loader.loadCover(request) }
        assertTrue(warm.isCompleted)
        assertSame(image, warm.await())
        assertEquals(1, reads)
        assertNull(loader.peekCover(request.copy(uri = "file:///covers/B.png")))
        assertNull(loader.peekCover(request.copy(size = BookCoverSize.forBounds(240, 360))))
        assertSame(image, loader.peekCover(request.copy(size = BookCoverSize.forBounds(119, 179))))
    }

    @Test fun cacheEvictsLeastRecentlyUsedImagesByBytesWithinFourMiB() = runTest {
        val loader = BookCoverLoader(
            BookCoverSource { DecodedBookCover(image, 2 * 1024 * 1024) },
            StandardTestDispatcher(testScheduler),
        )
        val second = request.copy(uri = "file:///covers/B.png")
        val third = request.copy(uri = "file:///covers/C.png")
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
            BookCoverSource {
                reads++
                release.await()
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
        )
        val first = async { loader.loadCover(request) }
        val second = async { loader.loadCover(request) }
        runCurrent()
        assertEquals(1, reads)
        release.complete(Unit)
        assertSame(image, first.await())
        assertSame(image, second.await())
        assertEquals(1, reads)
    }

    @Test fun clearingWhileDecodeIsPendingDoesNotRepopulateOldCache() = runTest {
        val release = CompletableDeferred<Unit>()
        val loader = BookCoverLoader(
            BookCoverSource {
                release.await()
                DecodedBookCover(image, 1024)
            },
            StandardTestDispatcher(testScheduler),
        )
        val pending = async { loader.loadCover(request) }
        runCurrent()
        loader.clear()
        release.complete(Unit)
        assertNull(pending.await())
        assertNull(loader.peekCover(request))
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
}
