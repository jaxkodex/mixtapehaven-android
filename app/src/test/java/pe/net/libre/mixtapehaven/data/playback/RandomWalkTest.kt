package pe.net.libre.mixtapehaven.data.playback

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.net.libre.mixtapehaven.model.Track

/** Unit coverage for the Random Walk dedupe filter (#128). */
class RandomWalkTest {

    private fun track(id: String?) =
        Track(title = "t", artist = "a", durationLabel = "0:00", artColor = Color(0xFF000000), id = id)

    @Test
    fun `filterRecent drops ids in the recent window`() {
        val tracks = listOf(track("1"), track("2"), track("3"))

        val fresh = filterRecent(tracks, recentIds = setOf("1", "3"))

        assertEquals(listOf("2"), fresh.map { it.id })
    }

    @Test
    fun `filterRecent keeps everything when the recent window is empty`() {
        val tracks = listOf(track("1"), track("2"))

        val fresh = filterRecent(tracks, recentIds = emptySet())

        assertEquals(listOf("1", "2"), fresh.map { it.id })
    }

    @Test
    fun `filterRecent keeps tracks without an id`() {
        // Null-id tracks can't be deduped; they pass through and are dropped later when unresolvable.
        val tracks = listOf(track(null), track("1"))

        val fresh = filterRecent(tracks, recentIds = setOf("1"))

        assertEquals(listOf(null), fresh.map { it.id })
    }
}
