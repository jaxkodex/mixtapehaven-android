package pe.net.libre.mixtapehaven.ui.nowplaying

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.playback.PlaybackState

/**
 * ViewModel for the Now Playing screen
 * Manages playback controls and user interactions
 */
class NowPlayingViewModel(
    private val playbackManager: PlaybackManager,
    private val onNavigateBack: () -> Unit
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    /**
     * Toggle play/pause
     */
    fun onPlayPauseClick() {
        playbackManager.togglePlayPause()
    }

    /**
     * Play next track
     */
    fun onNextClick() {
        playbackManager.playNext()
    }

    /**
     * Play previous track
     */
    fun onPreviousClick() {
        playbackManager.playPrevious()
    }

    /**
     * Seek to a specific position
     */
    fun onSeek(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    /**
     * Show lyrics
     * TODO: Implement lyrics display
     */
    fun onLyricsClick() {
        // TODO: Navigate to lyrics screen or show lyrics dialog
    }

    /**
     * Open equalizer
     * TODO: Implement equalizer
     */
    fun onEqualizerClick() {
        // TODO: Navigate to equalizer screen or show equalizer dialog
    }

    /**
     * Add to playlist
     * TODO: Implement add to playlist
     */
    fun onAddToPlaylistClick() {
        // TODO: Show playlist selection dialog
    }
}
