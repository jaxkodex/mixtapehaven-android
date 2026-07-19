package pe.net.libre.mixtapehaven.data.download

import pe.net.libre.mixtapehaven.model.VideoKind
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadedVideoMappingTest {

    private fun row(kind: String) = DownloadedVideo(
        id = "abc",
        title = "Dracula",
        kind = kind,
        seriesName = null,
        seasonEpisodeLabel = null,
        runtimeLabel = "1h 14m",
        posterUrl = null,
        artColorArgb = 0xFF6C8A7A.toInt(),
        qualityLabel = "720p",
        filePath = "/data/x.mp4",
        sizeBytes = 1_000,
        complete = true,
        status = VideoDownloadStatus.COMPLETE.name,
    )

    @Test
    fun `round-trips the persisted kind back into the domain model`() {
        assertEquals(VideoKind.EPISODE, row("EPISODE").toVideoItem().kind)
        assertEquals(VideoKind.MOVIE, row("MOVIE").toVideoItem().kind)
    }

    @Test
    fun `an unknown persisted kind degrades to movie instead of crashing`() {
        assertEquals(VideoKind.MOVIE, row("HOLOGRAM").toVideoItem().kind)
    }

    @Test
    fun `maps display fields through unchanged`() {
        val item = row("MOVIE").toVideoItem()
        assertEquals("Dracula", item.title)
        assertEquals("1h 14m", item.runtimeLabel)
    }

    @Test
    fun `download status round-trips by name`() {
        VideoDownloadStatus.entries.forEach { status ->
            assertEquals(status, VideoDownloadStatus.fromName(status.name))
        }
    }

    @Test
    fun `an unknown persisted status degrades to failed so the UI offers retry`() {
        assertEquals(VideoDownloadStatus.FAILED, VideoDownloadStatus.fromName("TELEPORTING"))
    }
}
