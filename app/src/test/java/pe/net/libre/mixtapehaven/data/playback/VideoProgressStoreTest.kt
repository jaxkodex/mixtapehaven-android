package pe.net.libre.mixtapehaven.data.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.mixtapehaven.data.download.VideoProgress
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for the local/server resume arbitration behind offline video playback. */
class VideoProgressStoreTest {

    private fun local(positionMs: Long, updatedAtMs: Long) = VideoProgress(
        id = "v1",
        positionMs = positionMs,
        runtimeMs = HOUR_MS,
        updatedAtMs = updatedAtMs,
        title = "Title",
        kind = VideoKind.MOVIE.name,
        seriesName = null,
        seasonEpisodeLabel = null,
        posterUrl = null,
        backdropUrl = null,
        artColorArgb = 0,
    )

    private fun remote(positionMs: Long, lastPlayedAtMs: Long) = VideoItem(
        id = "v1",
        title = "Title",
        kind = VideoKind.MOVIE,
        runtimeMs = HOUR_MS,
        resumePositionMs = positionMs,
        lastPlayedAtMs = lastPlayedAtMs,
    )

    @Test
    fun `no records resume from the start`() {
        assertEquals(0L, resolveResumePosition(null, null))
    }

    @Test
    fun `only a server record uses the server position`() {
        assertEquals(5_000L, resolveResumePosition(null, remote(5_000, lastPlayedAtMs = 100)))
    }

    @Test
    fun `only a local record uses the local position`() {
        assertEquals(7_000L, resolveResumePosition(local(7_000, updatedAtMs = 100), null))
    }

    /** Watching offline then reopening: the local write is newer, so it must not be overwritten. */
    @Test
    fun `the fresher local record wins over a stale server position`() {
        val resolved = resolveResumePosition(
            local(600_000, updatedAtMs = 2_000),
            remote(60_000, lastPlayedAtMs = 1_000),
        )

        assertEquals(600_000L, resolved)
    }

    /** Watching on another device: the server is newer even though its position is smaller. */
    @Test
    fun `the fresher server record wins even when its position is smaller`() {
        val resolved = resolveResumePosition(
            local(600_000, updatedAtMs = 1_000),
            remote(60_000, lastPlayedAtMs = 2_000),
        )

        assertEquals(60_000L, resolved)
    }

    /** A deliberate rewind must stick — taking the maximum position would undo it. */
    @Test
    fun `a rewind on the fresher side is preserved`() {
        val resolved = resolveResumePosition(
            local(30_000, updatedAtMs = 5_000),
            remote(1_800_000, lastPlayedAtMs = 4_000),
        )

        assertEquals(30_000L, resolved)
    }

    @Test
    fun `equal timestamps prefer the local record`() {
        val resolved = resolveResumePosition(
            local(30_000, updatedAtMs = 1_000),
            remote(90_000, lastPlayedAtMs = 1_000),
        )

        assertEquals(30_000L, resolved)
    }

    @Test
    fun `a title watched past the threshold counts as finished`() {
        assertTrue(isFinished(positionMs = (HOUR_MS * 0.96).toLong(), runtimeMs = HOUR_MS))
    }

    @Test
    fun `a partially watched title is not finished`() {
        assertFalse(isFinished(positionMs = HOUR_MS / 2, runtimeMs = HOUR_MS))
    }

    /** An unknown duration (a still-loading transcode) must never mark a title watched. */
    @Test
    fun `an unknown runtime is never finished`() {
        assertFalse(isFinished(positionMs = 600_000, runtimeMs = 0))
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
