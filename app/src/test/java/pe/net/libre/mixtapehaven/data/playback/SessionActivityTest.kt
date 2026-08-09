package pe.net.libre.mixtapehaven.data.playback

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pe.net.libre.mixtapehaven.MainActivity

/**
 * The other half of the notification tap: what media3 is handed as the session activity. Covers
 * the original bug — a session with no content intent, so tapping the notification did nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private fun sessionIntent(): Intent = shadowOf(nowPlayingSessionActivity(context)).savedIntent

    @Test
    fun `targets the launcher activity`() {
        assertEquals(MainActivity::class.java.name, sessionIntent().component?.className)
    }

    @Test
    fun `asks for now playing`() {
        assertTrue(sessionIntent().getBooleanExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, false))
    }

    /** Launching from a service context requires its own task. */
    @Test
    fun `starts a new task`() {
        assertTrue(sessionIntent().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    /** Required on API 31+, and the intent carries no data the receiver should be able to alter. */
    @Test
    fun `is immutable`() {
        assertTrue(shadowOf(nowPlayingSessionActivity(context)).isImmutable)
    }
}
