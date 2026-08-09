package pe.net.libre.mixtapehaven

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.net.libre.mixtapehaven.ui.navigation.Routes

/**
 * The deep-link half of "tapping the media notification opens the app". Runs against a plain
 * [Application] so the real one's dependency container (Room, DataStore, Jellyfin SDK) is not
 * built for what is only intent parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NowPlayingRequestTest {

    private fun notificationIntent() =
        Intent().putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)

    @Test
    fun `notification intent requests now playing`() {
        assertEquals(Routes.NOW_PLAYING, consumeNowPlayingRequest(notificationIntent()))
    }

    @Test
    fun `a plain launch intent requests nothing`() {
        assertNull(consumeNowPlayingRequest(Intent()))
    }

    @Test
    fun `reading strips the extra from the intent`() {
        val intent = notificationIntent()
        consumeNowPlayingRequest(intent)
        assertFalse(intent.hasExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING))
    }

    /**
     * The regression behind the review finding: the activity retains this intent, and the system
     * replays it on every recreation (rotation, Recents after a process kill). A second read must
     * not navigate, or the user is yanked back to Now Playing from wherever they were.
     */
    @Test
    fun `a replayed intent does not request again`() {
        val intent = notificationIntent()
        assertEquals(Routes.NOW_PLAYING, consumeNowPlayingRequest(intent))
        assertNull(consumeNowPlayingRequest(intent))
    }
}
