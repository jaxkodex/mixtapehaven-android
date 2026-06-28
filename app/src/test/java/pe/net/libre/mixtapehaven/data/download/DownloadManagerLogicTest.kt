package pe.net.libre.mixtapehaven.data.download

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.mixtapehaven.model.Track

/** Unit coverage for the pure download/local-first logic introduced for #129. */
class DownloadManagerLogicTest {

    private fun track(id: String?) =
        Track(title = "t", artist = "a", durationLabel = "0:00", artColor = Color(0xFF000000), id = id)

    @Test
    fun `resolveLocalFirst prefers the local file when downloaded`() {
        val downloaded = mapOf("1" to "file:///data/1")

        val url = resolveLocalFirst(
            track = track("1"),
            localUri = { downloaded[it.id] },
            streamUrl = { "stream://$it" },
        )

        assertEquals("file:///data/1", url)
    }

    @Test
    fun `resolveLocalFirst falls back to the stream when not downloaded`() {
        val downloaded = emptyMap<String, String>()

        val url = resolveLocalFirst(
            track = track("2"),
            localUri = { downloaded[it.id] },
            streamUrl = { "stream://$it" },
        )

        assertEquals("stream://2", url)
    }

    @Test
    fun `resolveLocalFirst returns null when there is no id and no local file`() {
        val url = resolveLocalFirst(
            track = track(null),
            localUri = { null },
            streamUrl = { "stream://$it" },
        )

        assertNull(url)
    }

    @Test
    fun `shouldStartDownload only starts when free space exceeds the floor`() {
        assertTrue(shouldStartDownload(freeBytes = 500_000_000, minFreeBytes = 200_000_000))
        assertFalse(shouldStartDownload(freeBytes = 100_000_000, minFreeBytes = 200_000_000))
        assertFalse(shouldStartDownload(freeBytes = 200_000_000, minFreeBytes = 200_000_000))
    }

    @Test
    fun `percentOf clamps and handles unknown totals`() {
        assertEquals(0, percentOf(downloaded = 0, total = 100))
        assertEquals(50, percentOf(downloaded = 50, total = 100))
        assertEquals(100, percentOf(downloaded = 100, total = 100))
        // Unknown content length reports 0% rather than crashing.
        assertEquals(0, percentOf(downloaded = 50, total = -1))
        // Never exceeds 100 even if more bytes arrive than advertised.
        assertEquals(100, percentOf(downloaded = 150, total = 100))
    }

    @Test
    fun `formatBytes renders human-readable sizes`() {
        assertEquals("0 MB", formatBytes(0))
        assertEquals("9.8 MB", formatBytes(9_800_000))
        assertEquals("14 MB", formatBytes(14_000_000))
        assertEquals("1.2 GB", formatBytes(1_200_000_000))
    }
}
