package pe.net.libre.mixtapehaven.data.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.C
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
            // Hold CPU + WiFi awake only while playing, so streaming survives screen-off/doze.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(nowPlayingIntent())
            .build()
    }

    /**
     * Where a tap on the media notification — or on the system's Now Playing tile — lands.
     * Without a session activity media3 builds the notification with no content intent, so
     * tapping it does nothing at all. [MainActivity] is `singleTask`, so this brings the
     * existing task forward and is delivered to `onNewIntent` rather than restarting the app.
     */
    private fun nowPlayingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
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
