package pe.net.libre.mixtapehaven.data.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import pe.net.libre.mixtapehaven.MainActivity
import pe.net.libre.mixtapehaven.R
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import java.util.concurrent.TimeUnit

/**
 * Foreground service that manages audio playback in the background.
 * This service keeps the app's audio playing even when the app is in the background.
 */
class MediaPlaybackService : Service() {

    private val binder = LocalBinder()
    private lateinit var playbackManager: PlaybackManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var imageLoader: ImageLoader
    private var coverArtBitmap: Bitmap? = null
    private var currentCoverUrl: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaPlaybackService onCreate")

        // Initialize ImageLoader after service is fully initialized
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        imageLoader = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()

        // Initialize PlaybackManager if not already initialized
        val dataStoreManager = DataStoreManager(applicationContext)
        playbackManager = PlaybackManager.getInstance()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        initializeMediaSession()
        setupPlayerListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playbackManager.resume()
                }

                override fun onPause() {
                    playbackManager.pause()
                }

                override fun onStop() {
                    playbackManager.stop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    playbackManager.seekTo(pos)
                }
            })
            isActive = true
        }
    }

    private fun setupPlayerListener() {
        playbackManager.player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
                updateMediaSessionPlaybackState(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> updateNotification()
                    // Don't stop service on STATE_ENDED - let PlaybackManager handle auto-play
                    // The service will be stopped when user explicitly stops playback
                }
            }
        })
    }

    private fun updateMediaSessionPlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, playbackManager.player.currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val playbackState = playbackManager.playbackState.value
        val song = playbackState.currentSong

        // Load cover art if available and URL has changed
        song?.albumCoverUrl?.let { coverUrl ->
            if (coverUrl != currentCoverUrl) {
                currentCoverUrl = coverUrl
                loadCoverArt(coverUrl)
            }
        }

        // Intent to open the app when notification is clicked
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause action
        val playPauseAction = if (playbackState.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        // Stop action
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_delete,
            "Stop",
            createPendingIntent(ACTION_STOP)
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "Mixtape Haven")
            .setContentText(song?.artist ?: "No song playing")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(playbackState.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Set large icon (cover art) if available
        coverArtBitmap?.let {
            notificationBuilder.setLargeIcon(it)
        }

        return notificationBuilder.build()
    }

    private fun loadCoverArt(url: String) {
        serviceScope.launch {
            try {
                val request = ImageRequest.Builder(this@MediaPlaybackService)
                    .data(url)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = result.drawable?.toBitmap()
                if (bitmap != null) {
                    coverArtBitmap = bitmap
                    // Don't call updateNotification() here to avoid recursive calls
                    // The notification will be updated on the next player event
                } else {
                    Log.e(TAG, "Failed to load cover art: drawable is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cover art: ${e.message}", e)
            }
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_PLAY -> playbackManager.resume()
            ACTION_PAUSE -> playbackManager.pause()
            ACTION_STOP -> {
                playbackManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        mediaSession.release()
        playbackManager.release()
    }

    fun getPlaybackManager(): PlaybackManager = playbackManager

    fun startForegroundService() {
        startForeground(NOTIFICATION_ID, createNotification())
    }

    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_FOREGROUND = "pe.net.libre.mixtapehaven.START_FOREGROUND"
        const val ACTION_PLAY = "pe.net.libre.mixtapehaven.PLAY"
        const val ACTION_PAUSE = "pe.net.libre.mixtapehaven.PAUSE"
        const val ACTION_STOP = "pe.net.libre.mixtapehaven.STOP"
    }
}