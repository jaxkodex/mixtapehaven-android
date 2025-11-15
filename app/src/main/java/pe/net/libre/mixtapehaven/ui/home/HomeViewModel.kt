package pe.net.libre.mixtapehaven.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository

/**
 * ViewModel for the home screen
 * Loads data from Jellyfin API via MediaRepository
 */
class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
    private val onNavigateToAllAlbums: () -> Unit = {},
    private val onNavigateToAllArtists: () -> Unit = {},
    private val onNavigateToAllSongs: () -> Unit = {},
    private val onNavigateToAllPlaylists: () -> Unit = {},
    private val onNavigateToNowPlaying: () -> Unit = {},
    private val onLogout: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
        observePlaybackState()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { playbackState ->
                // Update UI state when playback changes
                // This keeps the UI in sync with playback state
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load all data in parallel
                val recentAlbumsResult = mediaRepository.getRecentlyAddedAlbums(limit = 10)
                val topArtistsResult = mediaRepository.getTopArtists(limit = 10)
                val playlistsResult = mediaRepository.getUserPlaylists(limit = 10)
                val popularSongsResult = mediaRepository.getPopularSongs(limit = 20)

                // Update UI state with results
                _uiState.value = HomeUiState(
                    recentlyAddedAlbums = recentAlbumsResult.getOrElse { emptyList() },
                    topArtists = topArtistsResult.getOrElse { emptyList() },
                    playlists = playlistsResult.getOrElse { emptyList() },
                    popularSongs = popularSongsResult.getOrElse { emptyList() },
                    nowPlayingSong = null, // TODO: Implement now playing
                    isLoading = false
                )

                // Handle errors
                val errors = listOfNotNull(
                    recentAlbumsResult.exceptionOrNull(),
                    topArtistsResult.exceptionOrNull(),
                    playlistsResult.exceptionOrNull(),
                    popularSongsResult.exceptionOrNull()
                )

                if (errors.isNotEmpty()) {
                    _errorMessage.value = "Failed to load some content: ${errors.first().message}"
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.value = "Failed to load data: ${e.message}"
            }
        }
    }

    fun retry() {
        _errorMessage.value = null
        loadData()
    }

    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album details
    }

    fun onArtistClick(artist: Artist) {
        // TODO: Navigate to artist details
    }

    fun onSongClick(song: Song) {
        // Play the song
        playbackManager.playSong(song)
    }

    fun onPlayPauseClick() {
        // Toggle play/pause
        playbackManager.togglePlayPause()
    }

    fun onNowPlayingBarClick() {
        // Navigate to now playing screen
        onNavigateToNowPlaying()
    }

    fun onPlaylistClick(playlist: Playlist) {
        // TODO: Navigate to playlist details
    }

    fun onSeeMoreClick(section: String) {
        when (section) {
            "recently_added" -> onNavigateToAllAlbums()
            "top_artists" -> onNavigateToAllArtists()
            "playlists" -> onNavigateToAllPlaylists()
            "popular_songs" -> onNavigateToAllSongs()
        }
    }

    fun onSearchClick() {
        // TODO: Navigate to search screen
    }

    fun onProfileClick() {
        // Trigger logout
        onLogout()
    }
}

/**
 * UI state for the home screen
 */
data class HomeUiState(
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val nowPlayingSong: Song? = null,
    val isLoading: Boolean = true
)
