package pe.net.libre.mixtapehaven.ui.artist

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.common.BasePlaybackViewModel
import pe.net.libre.mixtapehaven.ui.common.calculateTotalDuration
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.home.Artist
import pe.net.libre.mixtapehaven.ui.home.Song

enum class ArtistTab {
    ALBUMS, SONGS
}

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMix: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: ArtistTab = ArtistTab.ALBUMS,
    val totalDuration: String = "0 hr 0 min"
)

class ArtistDetailViewModel(
    private val artistId: String,
    private val mediaRepository: MediaRepository,
    playbackManager: PlaybackManager
) : BasePlaybackViewModel(playbackManager) {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    override fun getSongs(): List<Song> = _uiState.value.songs

    init {
        loadArtistData()
    }

    private fun loadArtistData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // Load artist details, albums, and songs in parallel
            val artistDeferred = async { mediaRepository.getArtistById(artistId) }
            val albumsDeferred = async { mediaRepository.getArtistAlbums(artistId) }
            val songsDeferred = async { mediaRepository.getArtistSongs(artistId) }

            // Wait for all results
            val artistResult = artistDeferred.await()
            val albumsResult = albumsDeferred.await()
            val songsResult = songsDeferred.await()

            // Handle artist result
            artistResult.fold(
                onSuccess = { artist ->
                    _uiState.value = _uiState.value.copy(artist = artist)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load artist"
                    )
                    return@launch
                }
            )

            // Handle albums result (don't fail if albums fail to load)
            albumsResult.fold(
                onSuccess = { albums ->
                    _uiState.value = _uiState.value.copy(albums = albums)
                },
                onFailure = { _ ->
                    // Log but don't fail completely - show what we have
                }
            )

            // Handle songs result
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
                        errorMessage = error.message ?: "Failed to load artist songs"
                    )
                }
            )
        }
    }

    fun retry() {
        loadArtistData()
    }

    fun onTabChange(tab: ArtistTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onInstantMixClick() {
        performInstantMix(
            fetchMix = { mediaRepository.getArtistInstantMix(artistId) },
            onStartLoading = { _uiState.update { it.copy(isLoadingMix = true, errorMessage = null) } },
            onError = { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Failed to generate instant mix") } },
            onStopLoading = { _uiState.update { it.copy(isLoadingMix = false) } }
        )
    }

    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album details
    }
}
