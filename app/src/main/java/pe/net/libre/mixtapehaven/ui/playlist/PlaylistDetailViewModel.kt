package pe.net.libre.mixtapehaven.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.home.Song

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMix: Boolean = false,
    val errorMessage: String? = null,
    val totalDuration: String = "0 hr 0 min"
)

class PlaylistDetailViewModel(
    private val playlistId: String,
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlaylistData()
    }

    private fun loadPlaylistData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // Load playlist details and items in parallel
            val playlistResult = mediaRepository.getPlaylistById(playlistId)
            val songsResult = mediaRepository.getPlaylistItems(playlistId)

            playlistResult.fold(
                onSuccess = { playlist ->
                    _uiState.value = _uiState.value.copy(playlist = playlist)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load playlist"
                    )
                    return@launch
                }
            )

            songsResult.fold(
                onSuccess = { songs ->
                    _uiState.value = _uiState.value.copy(
                        songs = songs,
                        isLoading = false,
                        totalDuration = calculateTotalDuration(songs)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load playlist items"
                    )
                }
            )
        }
    }

    fun retry() {
        loadPlaylistData()
    }

    fun onPlayAllClick() {
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            playbackManager.setQueue(songs, startIndex = 0)
        }
    }

    fun onShuffleClick() {
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            val shuffledSongs = songs.shuffled()
            playbackManager.setQueue(shuffledSongs, startIndex = 0)
        }
    }

    fun onInstantMixClick() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMix = true, errorMessage = null)
            mediaRepository.getPlaylistInstantMix(playlistId)
                .onSuccess { songs ->
                    if (songs.isNotEmpty()) {
                        playbackManager.setQueue(songs, 0)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to generate instant mix"
                    )
                }
            _uiState.value = _uiState.value.copy(isLoadingMix = false)
        }
    }

    fun onSongClick(song: Song) {
        val songs = _uiState.value.songs
        val index = songs.indexOf(song)
        if (index != -1) {
            // Set queue starting from the clicked song
            playbackManager.setQueue(songs, startIndex = index)
        } else {
            // Fallback to just playing the song
            playbackManager.playSong(song)
        }
    }

    fun onPlayPauseClick() {
        playbackManager.togglePlayPause()
    }

    private fun calculateTotalDuration(songs: List<Song>): String {
        // Parse duration strings (format: "M:SS") and sum them
        var totalSeconds = 0
        songs.forEach { song ->
            val parts = song.duration.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                totalSeconds += (minutes * 60) + seconds
            }
        }

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return if (hours > 0) {
            "$hours hr $minutes min"
        } else {
            "$minutes min"
        }
    }
}
