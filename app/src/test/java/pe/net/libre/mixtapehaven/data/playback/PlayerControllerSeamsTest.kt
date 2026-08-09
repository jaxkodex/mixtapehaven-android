package pe.net.libre.mixtapehaven.data.playback

import androidx.compose.ui.graphics.Color
import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.mixtapehaven.model.Track

/** Unit coverage for the stream-resolver and queue-refill seams introduced for #128/#129. */
class PlayerControllerSeamsTest {

    private fun track(id: String?, title: String = "t") =
        Track(title = title, artist = "a", durationLabel = "0:00", artColor = Color(0xFF000000), id = id)

    @Test
    fun `resolvePlayables uses the provided resolver`() {
        val tracks = listOf(track("1"), track("2"))

        val playables = resolvePlayables(tracks) { "stream://${it.id}" }

        assertEquals(
            listOf("stream://1", "stream://2"),
            playables.map { it.url },
        )
        assertEquals(listOf("1", "2"), playables.map { it.id })
    }

    @Test
    fun `resolvePlayables drops tracks without an id`() {
        val tracks = listOf(track(null), track("2"))

        val playables = resolvePlayables(tracks) { "stream://${it.id}" }

        assertEquals(listOf("2"), playables.map { it.id })
    }

    @Test
    fun `resolvePlayables drops tracks the resolver cannot resolve`() {
        val tracks = listOf(track("1"), track("2"))

        // Resolver swap: e.g. only locally-downloaded tracks resolve to a file URL.
        val playables = resolvePlayables(tracks) { if (it.id == "2") "file://2" else null }

        assertEquals(listOf("2"), playables.map { it.id })
        assertEquals(listOf("file://2"), playables.map { it.url })
    }

    @Test
    fun `shouldRefillQueue triggers only when remaining items fall below threshold`() {
        // Queue of 5, threshold 3: refill once 3 or fewer items remain after current.
        assertFalse(shouldRefillQueue(currentIndex = 0, itemCount = 5, threshold = 3))
        assertTrue(shouldRefillQueue(currentIndex = 2, itemCount = 5, threshold = 3))
        assertTrue(shouldRefillQueue(currentIndex = 4, itemCount = 5, threshold = 3))
    }

    @Test
    fun `shouldRefillQueue never refills an empty queue`() {
        // A stopped/cleared player reports itemCount 0; it must stay stopped, not refill.
        assertFalse(shouldRefillQueue(currentIndex = 0, itemCount = 0, threshold = 3))
    }

    @Test
    fun `wakeModeForUri takes the WiFi lock only for streamed items`() {
        assertEquals(C.WAKE_MODE_NETWORK, wakeModeForUri("https://jellyfin.example/Audio/1/stream"))
        assertEquals(C.WAKE_MODE_NETWORK, wakeModeForUri("http://jellyfin.example/Audio/1/stream"))
    }

    @Test
    fun `wakeModeForUri leaves the WiFi radio alone for a downloaded file`() {
        // The drain this exists to stop: a downloaded album played with the screen off held the
        // Wi-Fi radio out of power-save for its whole run, for bytes already on disk.
        val saved = "file:///data/user/0/pe.net.libre.mixtapehaven/files/downloads/1"
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeForUri(saved))
    }

    @Test
    fun `wakeModeForUri treats an unknown or missing scheme as local`() {
        // Erring local costs a stream its WifiLock; erring network costs every offline listener
        // battery for nothing. The cheaper lock is the safer default.
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeForUri(null))
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeForUri("content://media/external/audio/media/42"))
    }
}
