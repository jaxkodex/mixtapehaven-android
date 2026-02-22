package pe.net.libre.mixtapehaven.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Playlist

class PlaylistActionViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    data class UiState(
        val playlists: List<Playlist> = emptyList(),
        val isLoadingPlaylists: Boolean = false,
        val isCreatingPlaylist: Boolean = false,
        val isAddingSong: Boolean = false,
        val resultMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Load user's playlists from the server
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPlaylists = true)
            
            val result = mediaRepository.getUserPlaylists(limit = 100)
            
            result.fold(
                onSuccess = { playlists ->
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        isLoadingPlaylists = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPlaylists = false,
                        resultMessage = "Failed to load playlists: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Add a song to an existing playlist
     */
    fun addSongToPlaylist(songId: String, playlistId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingSong = true)
            
            val result = mediaRepository.addSongToPlaylist(playlistId, songId)
            
            result.fold(
                onSuccess = {
                    val playlistName = _uiState.value.playlists.find { it.id == playlistId }?.name ?: "playlist"
                    _uiState.value = _uiState.value.copy(
                        isAddingSong = false,
                        resultMessage = "Added to $playlistName"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAddingSong = false,
                        resultMessage = "Failed to add song: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Create a new playlist and add a song to it
     */
    fun createPlaylistAndAddSong(name: String, songId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPlaylist = true)
            
            // Create playlist with the song
            val createResult = mediaRepository.createPlaylist(name, listOf(songId))
            
            createResult.fold(
                onSuccess = { _ ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPlaylist = false,
                        resultMessage = "Created \"$name\""
                    )
                    // Refresh playlists list to include the new one
                    loadPlaylists()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPlaylist = false,
                        resultMessage = "Failed to create playlist: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Clear the result message (call after showing snackbar)
     */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(resultMessage = null)
    }
}
