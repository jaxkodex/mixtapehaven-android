package pe.net.libre.mixtapehaven.ui.screens.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for the series-resume selection behind the detail screen's Play/Resume button. */
class SelectPlayTargetTest {

    private fun video(id: String, kind: VideoKind, resumeMs: Long = 0) =
        VideoItem(id = id, title = id, kind = kind, resumePositionMs = resumeMs)

    @Test
    fun `null item yields no target`() {
        assertNull(selectPlayTarget(null, emptyList()))
    }

    @Test
    fun `movie plays itself regardless of episodes`() {
        val movie = video("m", VideoKind.MOVIE)

        assertEquals(movie, selectPlayTarget(movie, listOf(video("e1", VideoKind.EPISODE))))
    }

    @Test
    fun `series plays the first in-progress episode`() {
        val series = video("s", VideoKind.SERIES)
        val episodes = listOf(
            video("e1", VideoKind.EPISODE),
            video("e2", VideoKind.EPISODE, resumeMs = 60_000),
            video("e3", VideoKind.EPISODE, resumeMs = 5_000),
        )

        assertEquals("e2", selectPlayTarget(series, episodes)?.id)
    }

    @Test
    fun `series with nothing in progress falls back to the first episode`() {
        val series = video("s", VideoKind.SERIES)
        val episodes = listOf(video("e1", VideoKind.EPISODE), video("e2", VideoKind.EPISODE))

        assertEquals("e1", selectPlayTarget(series, episodes)?.id)
    }

    @Test
    fun `series without episodes yields no target`() {
        assertNull(selectPlayTarget(video("s", VideoKind.SERIES), emptyList()))
    }

    @Test
    fun `series prefers the server's next up over the episode scan`() {
        val series = video("s", VideoKind.SERIES)
        val episodes = listOf(
            video("e1", VideoKind.EPISODE, resumeMs = 60_000),
            video("e2", VideoKind.EPISODE),
        )

        assertEquals("e2", selectPlayTarget(series, episodes, nextUp = video("e2", VideoKind.EPISODE))?.id)
    }

    /** The regression this fixes: a finished E1 has no position, so the scan would replay it. */
    @Test
    fun `series after finishing an episode plays the following one`() {
        val series = video("s", VideoKind.SERIES)
        // Jellyfin zeroes a watched episode's position, so nothing here looks in progress.
        val episodes = listOf(video("e1", VideoKind.EPISODE), video("e2", VideoKind.EPISODE))

        assertEquals("e2", selectPlayTarget(series, episodes, nextUp = video("e2", VideoKind.EPISODE))?.id)
    }

    @Test
    fun `next up is ignored for a movie`() {
        val movie = video("m", VideoKind.MOVIE)

        assertEquals(movie, selectPlayTarget(movie, emptyList(), nextUp = video("e2", VideoKind.EPISODE)))
    }
}
