package pe.net.libre.mixtapehaven.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the home screen
 * Currently uses mock data, will be connected to Jellyfin API in the future
 */
class HomeViewModel(
    private val onNavigateToAllAlbums: () -> Unit = {},
    private val onNavigateToAllArtists: () -> Unit = {},
    private val onNavigateToAllSongs: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _uiState.value = HomeUiState(
            recentlyAddedAlbums = mockRecentlyAddedAlbums,
            topArtists = mockTopArtists,
            popularSongs = mockPopularSongs,
            nowPlayingSong = mockNowPlayingSong,
            isLoading = false
        )
    }

    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album details
    }

    fun onArtistClick(artist: Artist) {
        // TODO: Navigate to artist details
    }

    fun onSongClick(song: Song) {
        // TODO: Play song or navigate to song details
    }

    fun onPlayPauseClick() {
        // TODO: Toggle play/pause
    }

    fun onNowPlayingBarClick() {
        // TODO: Navigate to now playing screen
    }

    fun onSeeMoreClick(section: String) {
        when (section) {
            "recently_added" -> onNavigateToAllAlbums()
            "top_artists" -> onNavigateToAllArtists()
            "popular_songs" -> onNavigateToAllSongs()
        }
    }

    fun onSearchClick() {
        // TODO: Navigate to search screen
    }

    fun onProfileClick() {
        // TODO: Open profile menu
    }
}

/**
 * UI state for the home screen
 */
data class HomeUiState(
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val nowPlayingSong: Song? = null,
    val isLoading: Boolean = true
)
