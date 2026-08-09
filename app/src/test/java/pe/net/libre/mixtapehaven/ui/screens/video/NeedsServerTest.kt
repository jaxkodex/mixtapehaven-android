package pe.net.libre.mixtapehaven.ui.screens.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for telling a saved copy apart from candidates that need the server, and for the
 * message shown when the server is the thing that is missing.
 */
class NeedsServerTest {

    @Test
    fun `stream candidates need the server`() {
        val candidates = listOf(
            "https://media.example/Videos/1/stream?static=true",
            "https://media.example/Videos/1/master.m3u8",
        )

        assertTrue(needsServer(candidates))
    }

    @Test
    fun `a saved copy plays without one`() {
        assertFalse(needsServer(listOf("file:///data/user/0/pe.net.libre.mixtapehaven/files/videos/1.mp4")))
    }

    /** The resolver serves a local file *instead of* streams, but don't depend on that ordering. */
    @Test
    fun `a local candidate anywhere in the list counts`() {
        assertFalse(needsServer(listOf("https://media.example/Videos/1/stream", "file:///videos/1.mp4")))
    }

    @Test
    fun `an empty list is treated as needing the server`() {
        assertTrue(needsServer(emptyList()))
    }

    /** Having a connection but no server is the case a plain "you're offline" would misdiagnose. */
    @Test
    fun `the message distinguishes no server from no network`() {
        assertTrue(unreachableMessage(hasNetwork = true).contains("server"))
        assertTrue(unreachableMessage(hasNetwork = false).contains("offline"))
    }
}
