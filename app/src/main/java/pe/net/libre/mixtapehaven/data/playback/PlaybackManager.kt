package pe.net.libre.mixtapehaven.data.playback

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import pe.net.libre.mixtapehaven.data.util.NetworkUtil
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    // Queue management
    private val queue = mutableListOf<Song>()
    private var currentIndex: Int = -1

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
    private val _player: ExoPlayer = createExoPlayer(context).apply {
        // Set up player listener to track state changes
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        // Media is buffering
                        Log.d(TAG, "Player is buffering")
                        _playbackState.value = _playbackState.value.copy(isBuffering = true)
                    }
                    Player.STATE_ENDED -> {
                        // Song finished
                        Log.d(TAG, "Song ended")
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                        stopProgressTracking()

                        // Auto-play next song if available
                        if (currentIndex < queue.size - 1) {
                            Log.d(TAG, "Auto-playing next song in queue")
                            playNext()
                        } else {
                            Log.d(TAG, "End of queue reached")
                        }
                    }
                    Player.STATE_READY -> {
                        // Media is ready to play
                        Log.d(TAG, "Player is ready")
                        val duration = _player.duration
                        _playbackState.value = _playbackState.value.copy(
                            duration = if (duration > 0) duration else _playbackState.value.duration,
                            isBuffering = false
                        )
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

    // Expose player for MediaPlaybackService
    internal val player: ExoPlayer get() = _player

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

                // Detect network type and select appropriate streaming quality
                val quality = getStreamingQuality()
                Log.d(TAG, "Network-based streaming quality: $quality")

                // Construct streaming URL with adaptive quality (auth handled via OkHttp headers)
                val streamUrl = song.getStreamUrl(serverUrl, quality)
                Log.d(TAG, "Streaming URL: $streamUrl")

                // Create media item
                val mediaItem = MediaItem.fromUri(streamUrl)

                // Set media item and prepare
                _player.setMediaItem(mediaItem)
                _player.prepare()
                _player.play()

                // Update state
                _playbackState.value = PlaybackState(
                    currentSong = song,
                    isPlaying = true,
                    isBuffering = true, // Show loading feedback immediately
                    currentPosition = 0L,
                    duration = parseDurationToMillis(song.duration), // Initial duration from song metadata
                    queue = queue.toList(),
                    currentIndex = currentIndex,
                    hasNext = currentIndex < queue.size - 1,
                    hasPrevious = currentIndex > 0
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
        if (_player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Pause playback
     */
    fun pause() {
        _player.pause()
    }

    /**
     * Resume playback
     */
    fun resume() {
        _player.play()
    }

    /**
     * Play next track in queue
     */
    fun playNext() {
        if (currentIndex < queue.size - 1) {
            Log.d(TAG, "Playing next track")
            playSongFromQueue(currentIndex + 1)
        } else {
            Log.d(TAG, "No next track available")
        }
    }

    /**
     * Play previous track in queue
     */
    fun playPrevious() {
        val currentPosition = _player.currentPosition
        if (currentPosition > 3000) {
            // If more than 3 seconds into the song, restart current song
            Log.d(TAG, "Restarting current track")
            seekTo(0L)
        } else if (currentIndex > 0) {
            // Otherwise, play previous song in queue
            Log.d(TAG, "Playing previous track")
            playSongFromQueue(currentIndex - 1)
        } else {
            // At the first song, just restart
            Log.d(TAG, "At first track, restarting")
            seekTo(0L)
        }
    }

    /**
     * Seek to a specific position
     */
    fun seekTo(positionMs: Long) {
        _player.seekTo(positionMs)
    }

    /**
     * Stop playback
     */
    fun stop() {
        _player.stop()
        _playbackState.value = PlaybackState()
        stopProgressTracking()
    }

    /**
     * Set queue and start playing from a specific index
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        Log.d(TAG, "setQueue called with ${songs.size} songs, startIndex=$startIndex")
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex.coerceIn(0, songs.size - 1)

        if (queue.isNotEmpty()) {
            playSongFromQueue(currentIndex)
        }
    }

    /**
     * Add song to the end of the queue
     */
    fun addToQueue(song: Song) {
        Log.d(TAG, "addToQueue: ${song.title}")
        queue.add(song)
        updateQueueState()
    }

    /**
     * Add multiple songs to the end of the queue
     */
    fun addToQueue(songs: List<Song>) {
        Log.d(TAG, "addToQueue: ${songs.size} songs")
        queue.addAll(songs)
        updateQueueState()
    }

    /**
     * Remove song from queue at index
     */
    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            Log.d(TAG, "removeFromQueue at index $index")
            queue.removeAt(index)

            // Adjust current index if needed
            if (index < currentIndex) {
                currentIndex--
            } else if (index == currentIndex && currentIndex >= queue.size) {
                currentIndex = queue.size - 1
            }

            updateQueueState()
        }
    }

    /**
     * Clear the queue
     */
    fun clearQueue() {
        Log.d(TAG, "clearQueue")
        queue.clear()
        currentIndex = -1
        updateQueueState()
    }

    /**
     * Get the current queue
     */
    fun getQueue(): List<Song> = queue.toList()

    /**
     * Play song from queue at specific index
     */
    private fun playSongFromQueue(index: Int) {
        if (index in queue.indices) {
            currentIndex = index
            playSong(queue[index])
            updateQueueState()
        }
    }

    /**
     * Update playback state with queue information
     */
    private fun updateQueueState() {
        _playbackState.value = _playbackState.value.copy(
            queue = queue.toList(),
            currentIndex = currentIndex,
            hasNext = currentIndex < queue.size - 1,
            hasPrevious = currentIndex > 0
        )
    }

    /**
     * Release resources
     */
    fun release() {
        _player.release()
        stopProgressTracking()
    }

    /**
     * Start tracking playback progress
     */
    private fun startProgressTracking() {
        stopProgressTracking()

        progressJob = scope.launch {
            while (isActive && _player.isPlaying) {
                delay(500) // Update every 500ms - reduces battery drain

                val currentPosition = _player.currentPosition
                val duration = _player.duration

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
     * Determine the appropriate streaming quality based on network conditions
     */
    private fun getStreamingQuality(): StreamingQuality {
        val networkType = NetworkUtil.getNetworkType(context)

        return when (networkType) {
            NetworkUtil.NetworkType.WIFI,
            NetworkUtil.NetworkType.ETHERNET -> {
                // High-speed connections: stream original quality
                Log.d(TAG, "High-speed connection detected, using ORIGINAL quality")
                StreamingQuality.ORIGINAL
            }
            NetworkUtil.NetworkType.CELLULAR -> {
                // Cellular connection: use medium quality (192kbps) for data savings
                Log.d(TAG, "Cellular connection detected, using MEDIUM quality (192kbps)")
                StreamingQuality.MEDIUM
            }
            NetworkUtil.NetworkType.NONE -> {
                // No connection: attempt original quality (will fail gracefully)
                Log.w(TAG, "No network connection detected, attempting ORIGINAL quality")
                StreamingQuality.ORIGINAL
            }
        }
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
            .setWakeMode(C.WAKE_MODE_NETWORK) // Efficient wake lock management for network streaming
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
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L, // milliseconds
    val duration: Long = 0L, // milliseconds
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
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
