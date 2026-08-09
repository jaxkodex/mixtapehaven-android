package pe.net.libre.mixtapehaven.ui.screens.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for what a Continue watching tap may do.
 *
 * The regression these guard: offline, every title in the rail navigated into the player, which
 * then sat on a black frame while stream candidates failed — indistinguishable from a dead tap.
 */
class CanResumeOfflineTest {

    @Test
    fun `online anything streams`() {
        assertTrue(canResumeOffline("a", online = true, downloadedIds = emptySet()))
    }

    @Test
    fun `offline a downloaded title still plays`() {
        assertTrue(canResumeOffline("a", online = false, downloadedIds = setOf("a")))
    }

    @Test
    fun `offline a title with no saved copy cannot play`() {
        assertFalse(canResumeOffline("a", online = false, downloadedIds = setOf("b")))
    }
}
