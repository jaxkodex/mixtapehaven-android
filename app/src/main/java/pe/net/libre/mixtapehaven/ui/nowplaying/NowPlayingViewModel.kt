package pe.net.libre.mixtapehaven.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.playback.PlaybackState
import pe.net.libre.mixtapehaven.data.repository.MediaRepository

data class NowPlayingUiState(
    val isLoadingMix: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Now Playing screen
 * Manages playback controls and user interactions
 */
class NowPlayingViewModel(
    private val playbackManager: PlaybackManager,
    private val mediaRepository: MediaRepository,
    private val onNavigateBack: () -> Unit
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

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
     * Start an instant mix based on the currently playing song
     */
    fun startInstantMix() {
        val currentSong = playbackManager.playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMix = true, errorMessage = null) }
            try {
                mediaRepository.getSongInstantMix(currentSong.id)
                    .onSuccess { songs ->
                        if (songs.isNotEmpty()) {
                            playbackManager.setQueue(songs, 0)
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "Failed to generate instant mix")
                        }
                    }
            } finally {
                _uiState.update { it.copy(isLoadingMix = false) }
            }
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
