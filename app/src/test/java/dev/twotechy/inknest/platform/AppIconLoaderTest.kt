package dev.twotechy.inknest.platform

import androidx.compose.ui.graphics.ImageBitmap
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppIconLoaderTest {
    private val request = AppIconRequest("reader/Activity", 1, "en-US", 320)

    // Cache tests need image identity only; no Android bitmap allocation or rendering is involved.
    private val image = Proxy.newProxyInstance(
        ImageBitmap::class.java.classLoader,
        arrayOf(ImageBitmap::class.java),
    ) { _, _, _ -> error("Cache lookup must not inspect pixels") } as ImageBitmap

    @Test fun warmIconIsAvailableSynchronouslyWithoutDispatchOrAnotherSourceRead() = runTest {
        var reads = 0
        val loader =
            AppIconLoader(
                AppIconSource {
                    reads++
                    image
                },
                StandardTestDispatcher(testScheduler),
            )
        assertNull(loader.peekIcon(request))
        assertEquals(0, reads)
        assertSame(image, loader.loadIcon(request))
        assertSame(image, loader.peekIcon(request))
        val warm = async(start = CoroutineStart.UNDISPATCHED) { loader.loadIcon(request) }
        assertTrue(warm.isCompleted)
        assertSame(image, warm.await())
        assertEquals(1, reads)
    }

    @Test fun requestChangesMissCacheAndCapacityRemainsOneHundredIcons() = runTest {
        var reads = 0
        val loader =
            AppIconLoader(
                AppIconSource {
                    reads++
                    image
                },
                StandardTestDispatcher(testScheduler),
            )
        val variants = listOf(
            request,
            request.copy(componentId = "other/Activity"),
            request.copy(version = 2),
            request.copy(locale = "vi-VN"),
            request.copy(densityDpi = 480),
        )
        for (variant in variants) {
            assertNull(loader.peekIcon(variant))
            assertSame(image, loader.loadIcon(variant))
            assertSame(image, loader.peekIcon(variant))
        }
        assertEquals(5, reads)
        loader.clear()
        assertNull(loader.peekIcon(request))
        repeat(101) { loader.loadIcon(request.copy(version = it.toLong())) }
        assertNull(loader.peekIcon(request.copy(version = 0)))
        assertSame(image, loader.peekIcon(request.copy(version = 100)))
        assertEquals(106, reads)
    }

    @Test fun cancellationPropagatesWithoutCachingAResult() = runTest {
        val cancelled = CancellationException("icon no longer visible")
        val loader =
            AppIconLoader(AppIconSource { throw cancelled }, StandardTestDispatcher(testScheduler))
        try {
            loader.loadIcon(request)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertEquals(cancelled.message, actual.message)
        }
        assertNull(loader.peekIcon(request))
    }
}
