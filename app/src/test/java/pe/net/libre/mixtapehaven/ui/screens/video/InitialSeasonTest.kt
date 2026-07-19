package pe.net.libre.mixtapehaven.ui.screens.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for which season a series detail screen opens on. */
class InitialSeasonTest {

    private fun episode(id: String) = VideoItem(id = id, title = id, kind = VideoKind.EPISODE)

    private val seasons = listOf(
        "Season 1" to listOf(episode("s1e1"), episode("s1e2")),
        "Season 2" to listOf(episode("s2e1")),
        "Season 3" to listOf(episode("s3e1")),
    )

    @Test
    fun `opens on the season holding next up`() {
        assertEquals("Season 3", initialSeason(seasons, episode("s3e1")))
    }

    @Test
    fun `falls back to the first season without next up`() {
        assertEquals("Season 1", initialSeason(seasons, nextUp = null))
    }

    /** A Next Up from a season the episode list does not contain must not blank the selection. */
    @Test
    fun `unknown next up falls back to the first season`() {
        assertEquals("Season 1", initialSeason(seasons, episode("ghost")))
    }

    @Test
    fun `no seasons yields no selection`() {
        assertNull(initialSeason(emptyList(), episode("s1e1")))
    }
}
