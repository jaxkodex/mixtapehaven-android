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

    @Test
    fun `formatTimeLeft renders minutes under an hour`() {
        assertEquals("23 min left", formatTimeLeft(runtimeMs = 60 * 60_000L, positionMs = 37 * 60_000L))
    }

    @Test
    fun `formatTimeLeft renders hours and minutes`() {
        assertEquals("1h 12m left", formatTimeLeft(runtimeMs = 120 * 60_000L, positionMs = 48 * 60_000L))
    }

    /** Under a minute left reads as finished, so the card drops the segment rather than saying "0 min". */
    @Test
    fun `formatTimeLeft is empty near the end`() {
        assertEquals("", formatTimeLeft(runtimeMs = 60 * 60_000L, positionMs = 60 * 60_000L))
        assertEquals("", formatTimeLeft(runtimeMs = 60 * 60_000L, positionMs = 59 * 60_000L + 30_000L))
    }

    @Test
    fun `formatTimeLeft is empty for an unknown runtime`() {
        assertEquals("", formatTimeLeft(runtimeMs = 0, positionMs = 0))
    }
}
