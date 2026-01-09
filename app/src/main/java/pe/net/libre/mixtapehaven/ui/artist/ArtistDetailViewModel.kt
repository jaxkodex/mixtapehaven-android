package pe.net.libre.mixtapehaven.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
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
    val errorMessage: String? = null,
    val selectedTab: ArtistTab = ArtistTab.ALBUMS,
    val totalDuration: String = "0 hr 0 min"
)

class ArtistDetailViewModel(
    private val artistId: String,
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

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
                onFailure = { error ->
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

    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album details
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
