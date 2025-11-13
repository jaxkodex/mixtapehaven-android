package pe.net.libre.mixtapehaven.data.playback

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.ui.home.Song
import java.util.concurrent.TimeUnit

/**
 * Manages playback state for the application using ExoPlayer
 * This is a singleton that maintains the current playback state
 * and provides methods to control playback
 */
class PlaybackManager private constructor(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    companion object {
        private const val TAG = "PlaybackManager"

        @Volatile
        private var instance: PlaybackManager? = null

        fun getInstance(context: Context, dataStoreManager: DataStoreManager): PlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackManager(
                    context.applicationContext,
                    dataStoreManager
                ).also { instance = it }
            }
        }

        fun getInstance(): PlaybackManager {
            return instance ?: throw IllegalStateException(
                "PlaybackManager must be initialized with getInstance(context, dataStoreManager) first"
            )
        }
    }

    // Device ID for authentication headers
    private val deviceId: String by lazy {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    // Current access token for authentication (updated when playing songs)
    @Volatile
    private var currentAccessToken: String? = null

    // ExoPlayer instance with custom OkHttp DataSource for authenticated streaming
    private val player: ExoPlayer = createExoPlayer(context).apply {
        // Set up player listener to track state changes
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        // Song finished
                        _playbackState.value = _playbackState.value.copy(isPlaying = false)
                        stopProgressTracking()
                        // TODO: Auto-play next song
                    }
                    Player.STATE_READY -> {
                        // Media is ready to play
                        val duration = player.duration
                        if (duration > 0) {
                            _playbackState.value = _playbackState.value.copy(duration = duration)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}", error)
                Log.e(TAG, "Error code: ${error.errorCode}")
                if (error.cause != null) {
                    Log.e(TAG, "Caused by: ${error.cause?.message}", error.cause)
                }
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopProgressTracking()
            }
        })
    }

    /**
     * Play a song
     */
    fun playSong(song: Song) {
        scope.launch {
            try {
                Log.d(TAG, "playSong called for: ${song.title} by ${song.artist}")

                // Get server URL and access token from DataStore
                val serverUrl = dataStoreManager.serverUrl.first()
                val accessToken = dataStoreManager.accessToken.first()

                if (serverUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                    // Can't play without server connection
                    Log.e(TAG, "Cannot play: serverUrl or accessToken is empty")
                    return@launch
                }

                // Update the current access token for the OkHttp interceptor
                currentAccessToken = accessToken
                Log.d(TAG, "Access token set for interceptor: ${accessToken.take(10)}...")

                // Construct streaming URL (auth handled via OkHttp headers)
                val streamUrl = song.getStreamUrl(serverUrl)
                Log.d(TAG, "Streaming URL: $streamUrl")

                // Create media item
                val mediaItem = MediaItem.fromUri(streamUrl)

                // Set media item and prepare
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                // Update state
                _playbackState.value = PlaybackState(
                    currentSong = song,
                    isPlaying = true,
                    currentPosition = 0L,
                    duration = parseDurationToMillis(song.duration) // Initial duration from song metadata
                )

                startProgressTracking()
            } catch (e: Exception) {
                Log.e(TAG, "Error in playSong: ${e.message}", e)
                e.printStackTrace()
                // TODO: Handle error (show toast/snackbar)
            }
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Pause playback
     */
    fun pause() {
        player.pause()
    }

    /**
     * Resume playback
     */
    fun resume() {
        player.play()
    }

    /**
     * Play next track
     * TODO: Implement queue management
     */
    fun playNext() {
        // For now, just stop playback
        // In the future, this will play the next song in the queue
        stop()
    }

    /**
     * Play previous track
     * TODO: Implement queue management
     */
    fun playPrevious() {
        // For now, restart the current song
        val currentPosition = player.currentPosition
        if (currentPosition > 3000) {
            // If more than 3 seconds, restart current song
            seekTo(0L)
        } else {
            // Otherwise, play previous song in queue
            // For now, just restart
            seekTo(0L)
        }
    }

    /**
     * Seek to a specific position
     */
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    /**
     * Stop playback
     */
    fun stop() {
        player.stop()
        _playbackState.value = PlaybackState()
        stopProgressTracking()
    }

    /**
     * Release resources
     */
    fun release() {
        player.release()
        stopProgressTracking()
    }

    /**
     * Start tracking playback progress
     */
    private fun startProgressTracking() {
        stopProgressTracking()

        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                delay(100) // Update every 100ms

                val currentPosition = player.currentPosition
                val duration = player.duration

                _playbackState.value = _playbackState.value.copy(
                    currentPosition = currentPosition,
                    duration = if (duration > 0) duration else _playbackState.value.duration
                )
            }
        }
    }

    /**
     * Stop tracking playback progress
     */
    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Parse duration string to milliseconds
     * Format: "MM:SS" or "M:SS"
     */
    private fun parseDurationToMillis(duration: String): Long {
        return try {
            val parts = duration.split(":")
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            (minutes * 60 + seconds) * 1000
        } catch (e: Exception) {
            // Default to 3 minutes if parsing fails
            180000L
        }
    }

    /**
     * Create ExoPlayer instance with custom OkHttp DataSource for authenticated streaming
     */
    private fun createExoPlayer(context: Context): ExoPlayer {
        // Create OkHttp client with dynamic authentication interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Dynamic auth interceptor that uses the current access token
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()

            // Only add auth headers if we have an access token
            val request = if (currentAccessToken != null) {
                val authHeader = createAuthorizationHeader()
                Log.d(TAG, "Adding auth headers to request: ${original.url}")
                Log.d(TAG, "X-Emby-Authorization: $authHeader")
                Log.d(TAG, "X-Emby-Token: ${currentAccessToken?.take(10)}...")
                original.newBuilder()
                    .header("X-Emby-Authorization", authHeader)
                    .header("X-Emby-Token", currentAccessToken!!)
                    .build()
            } else {
                Log.w(TAG, "No access token available for request: ${original.url}")
                original
            }

            val response = chain.proceed(request)
            Log.d(TAG, "Response code: ${response.code} for ${request.url}")
            if (!response.isSuccessful) {
                Log.w(TAG, "Request failed with code: ${response.code}, message: ${response.message}")
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    /**
     * Generate the X-Emby-Authorization header required by Jellyfin
     */
    private fun createAuthorizationHeader(
        clientName: String = "Mixtape Haven",
        deviceName: String = "Android",
        version: String = "1.0.0"
    ): String {
        return "MediaBrowser Client=\"$clientName\", Device=\"$deviceName\", " +
               "DeviceId=\"$deviceId\", Version=\"$version\""
    }
}

/**
 * Represents the current playback state
 */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L, // milliseconds
    val duration: Long = 0L // milliseconds
) {
    /**
     * Get progress as a float between 0.0 and 1.0
     */
    val progress: Float
        get() = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Format current position as MM:SS
     */
    val currentPositionFormatted: String
        get() = formatTime(currentPosition)

    /**
     * Format duration as MM:SS
     */
    val durationFormatted: String
        get() = formatTime(duration)

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
