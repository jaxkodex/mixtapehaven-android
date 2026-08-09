package pe.net.libre.mixtapehaven.ui.screens.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for what a Continue watching tap may do, and how it explains itself.
 *
 * The regression these guard: with the server out of reach, every title in the rail navigated into
 * the player, which then sat on a black frame while stream candidates failed — indistinguishable
 * from a dead tap.
 */
class ContinueWatchingTapTest {

    @Test
    fun `with the server reachable anything streams`() {
        assertTrue(canPlayNow("a", serverReachable = true, downloadedIds = emptySet()))
    }

    @Test
    fun `without a server a downloaded title still plays`() {
        assertTrue(canPlayNow("a", serverReachable = false, downloadedIds = setOf("a")))
    }

    @Test
    fun `without a server a title with no saved copy cannot play`() {
        assertFalse(canPlayNow("a", serverReachable = false, downloadedIds = setOf("b")))
    }

    /** The LAN-only / VPN-down case: blaming the connection would send the user looking in the wrong place. */
    @Test
    fun `an unreachable server on a working connection is named as such`() {
        val message = unavailableMessage("The Wire", hasNetwork = true)

        assertTrue(message.contains("The Wire"))
        assertTrue(message.contains("server"))
    }

    @Test
    fun `with no network the message says to connect`() {
        val message = unavailableMessage("The Wire", hasNetwork = false)

        assertTrue(message.contains("The Wire"))
        assertTrue(message.contains("connect"))
    }
}
