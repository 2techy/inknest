package dev.twotechy.inknest.data.source

import java.nio.file.Files
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookChecksumTest {
    @Test fun digestMatchesKoreaderSamplingIncludingShortAndSparseFiles() = runTest {
        val fixtures = mapOf(
            0 to "d41d8cd98f00b204e9800998ecf8427e",
            5 to "d05374dc381d9b52806446a71c8e79b1",
            1024 to "9ee0a0e0c0bc0f1ff29d663d1fdf0743",
            1025 to "3f3789452b88cb32b8cbfbafe715e29a",
            20000 to "04a4d10f5e5583d7173cce0782769cb1",
        )
        val file = Files.createTempFile("book-checksum", ".bin")
        try {
            for ((size, expected) in fixtures) {
                Files.write(file, ByteArray(size) { (it % 251).toByte() })
                Files.newByteChannel(file).use {
                    assertEquals("size=$size", expected, partialBookMd5(it))
                }
            }
        } finally {
            Files.delete(file)
        }
    }

    @Test fun cancellationDoesNotProduceACompletedDigest() = runTest {
        var completed = false
        val job = launch {
            cancel()
            val file = Files.createTempFile("book-checksum", ".bin")
            try {
                Files.newByteChannel(file).use { partialBookMd5(it) }
                completed = true
            } finally {
                Files.delete(file)
            }
        }
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(false, completed)
    }
}
