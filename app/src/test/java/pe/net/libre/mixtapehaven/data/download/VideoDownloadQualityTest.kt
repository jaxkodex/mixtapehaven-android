package pe.net.libre.mixtapehaven.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDownloadQualityTest {

    @Test
    fun `fromName resolves a persisted quality by enum name`() {
        assertEquals(VideoDownloadQuality.SD_480, VideoDownloadQuality.fromName("SD_480"))
        assertEquals(VideoDownloadQuality.HD_1080, VideoDownloadQuality.fromName("HD_1080"))
    }

    @Test
    fun `fromName falls back to the default for unknown values`() {
        assertEquals(VideoDownloadQuality.DEFAULT, VideoDownloadQuality.fromName("4K"))
    }

    @Test
    fun `fromName falls back to the default when nothing was persisted`() {
        assertEquals(VideoDownloadQuality.DEFAULT, VideoDownloadQuality.fromName(null))
    }
}
