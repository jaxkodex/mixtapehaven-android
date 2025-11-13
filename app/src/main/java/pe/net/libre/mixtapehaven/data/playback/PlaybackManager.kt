package pe.net.libre.mixtapehaven.data.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.ui.home.Song

/**
 * Manages playback state for the application using ExoPlayer
 * This is a singleton that maintains the current playback state
 * and provides methods to control playback
 */
class PlaybackManager private constructor(
    context: Context,
    private val dataStoreManager: DataStoreManager
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // ExoPlayer instance
    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
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
        })
    }

    /**
     * Play a song
     */
    fun playSong(song: Song) {
        scope.launch {
            try {
                // Get server URL and access token from DataStore
                val serverUrl = dataStoreManager.serverUrl.first()
                val accessToken = dataStoreManager.accessToken.first()

                if (serverUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                    // Can't play without server connection
                    return@launch
                }

                // Construct streaming URL
                val streamUrl = song.getStreamUrl(serverUrl, accessToken)

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

    companion object {
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
