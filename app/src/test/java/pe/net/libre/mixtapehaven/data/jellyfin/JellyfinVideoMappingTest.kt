package pe.net.libre.mixtapehaven.data.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinVideoMappingTest {

    @Test
    fun `formatRuntime renders hours and minutes`() {
        assertEquals("1h 14m", formatRuntime(74 * 60_000L))
        assertEquals("2h 0m", formatRuntime(120 * 60_000L))
    }

    @Test
    fun `formatRuntime renders minutes only under an hour`() {
        assertEquals("42m", formatRuntime(42 * 60_000L))
        assertEquals("1m", formatRuntime(60_000L))
    }

    @Test
    fun `formatRuntime is empty for zero or negative`() {
        assertEquals("", formatRuntime(0))
        assertEquals("", formatRuntime(-5))
    }

    @Test
    fun `videoColorFor is deterministic per key`() {
        assertEquals(videoColorFor("abc"), videoColorFor("abc"))
    }
}
