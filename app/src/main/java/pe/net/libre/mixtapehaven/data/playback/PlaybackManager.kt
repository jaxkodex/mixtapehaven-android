package pe.net.libre.mixtapehaven.data.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.ui.home.Song

/**
 * Manages playback state for the application
 * This is a singleton that maintains the current playback state
 * and provides methods to control playback
 */
class PlaybackManager {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    /**
     * Play a song
     */
    fun playSong(song: Song) {
        // Parse duration string (format: "MM:SS") to milliseconds
        val durationMs = parseDurationToMillis(song.duration)

        _playbackState.value = PlaybackState(
            currentSong = song,
            isPlaying = true,
            currentPosition = 0L,
            duration = durationMs
        )

        startProgressTracking()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        val currentState = _playbackState.value
        if (currentState.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Pause playback
     */
    fun pause() {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressTracking()
    }

    /**
     * Resume playback
     */
    fun resume() {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
        startProgressTracking()
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
        val currentState = _playbackState.value
        if (currentState.currentPosition > 3000) {
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
        val currentState = _playbackState.value
        _playbackState.value = currentState.copy(
            currentPosition = positionMs.coerceIn(0L, currentState.duration)
        )
    }

    /**
     * Stop playback
     */
    fun stop() {
        _playbackState.value = PlaybackState()
        stopProgressTracking()
    }

    /**
     * Start tracking playback progress
     */
    private fun startProgressTracking() {
        stopProgressTracking()

        progressJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                delay(100) // Update every 100ms

                val currentState = _playbackState.value
                val newPosition = currentState.currentPosition + 100

                if (newPosition >= currentState.duration) {
                    // Song finished
                    _playbackState.value = currentState.copy(
                        currentPosition = currentState.duration,
                        isPlaying = false
                    )
                    stopProgressTracking()
                    // TODO: Auto-play next song
                } else {
                    _playbackState.value = currentState.copy(
                        currentPosition = newPosition
                    )
                }
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

        fun getInstance(): PlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackManager().also { instance = it }
            }
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
