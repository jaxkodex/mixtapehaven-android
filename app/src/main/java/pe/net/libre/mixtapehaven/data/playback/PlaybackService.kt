package pe.net.libre.mixtapehaven.data.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import okhttp3.OkHttpClient
import pe.net.libre.mixtapehaven.MainActivity

/** Foreground [MediaSessionService] hosting the single ExoPlayer instance for the app. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val httpFactory = OkHttpDataSource.Factory(OkHttpClient())
        // Route file:// URIs (downloaded tracks) to a local FileDataSource and only
        // http(s) through OkHttp; OkHttp alone rejects non-http schemes.
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            // Hold the CPU awake only while playing, so playback survives screen-off/doze. The
            // Wi-Fi half is added per item below, and only when the item is actually streamed.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    player.setWakeMode(wakeModeForUri(mediaItem?.localConfiguration?.uri?.toString()))
                }
            },
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(nowPlayingSessionActivity(this))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

/**
 * The wake mode an item needs, from the URI its bytes come from.
 *
 * [C.WAKE_MODE_NETWORK] adds a WifiLock on top of the CPU lock, which holds the Wi-Fi radio out of
 * power-save for as long as the item plays. A stream needs that; a downloaded file does not, and
 * offline playback is precisely the long screen-off case where the wasted lock costs the most.
 * Anything that is not plainly http(s) — `file://`, `content://`, an unset URI — is treated as
 * local, so an unrecognised scheme errs towards the cheaper lock rather than the wasteful one.
 *
 * Pure (media3 constants only, no player or Android framework types) so the decision is
 * unit-coverable.
 */
internal fun wakeModeForUri(uri: String?): Int =
    if (uri != null && (uri.startsWith("http://", true) || uri.startsWith("https://", true))) {
        C.WAKE_MODE_NETWORK
    } else {
        C.WAKE_MODE_LOCAL
    }

/**
 * Where a tap on the media notification — or on the system's Now Playing tile — lands. Without a
 * session activity media3 builds the notification with no content intent, so tapping it does
 * nothing at all. [MainActivity] is `singleTask`, so this brings the existing task forward and is
 * delivered to `onNewIntent` rather than restarting the app.
 */
internal fun nowPlayingSessionActivity(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
