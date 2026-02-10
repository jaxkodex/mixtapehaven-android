package pe.net.libre.mixtapehaven.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.DownloadedSongMapper
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkUtil

/**
 * ViewModel for the home screen
 * Loads data from Jellyfin API via MediaRepository
 */
class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
    private val offlineRepository: OfflineRepository,
    private val dataStoreManager: DataStoreManager,
    private val context: Context,
    private val onNavigateToAllAlbums: () -> Unit = {},
    private val onNavigateToAllArtists: () -> Unit = {},
    private val onNavigateToAllSongs: () -> Unit = {},
    private val onNavigateToAllPlaylists: () -> Unit = {},
    private val onNavigateToPlaylistDetail: (String) -> Unit = {},
    private val onNavigateToArtistDetail: (String) -> Unit = {},
    private val onNavigateToNowPlaying: () -> Unit = {},
    private val onNavigateToSearch: () -> Unit = {},
    private val onLogout: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
        observePlaybackState()
        observeDownloadedSongs()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { playbackState ->
                // Update UI state when playback changes
                // This keeps the UI in sync with playback state
            }
        }
    }

    private fun observeDownloadedSongs() {
        viewModelScope.launch {
            offlineRepository.getAllDownloaded().collect { downloadedSongs ->
                // Update songs to reflect download status
                val downloadedIds = downloadedSongs.map { it.id }.toSet()
                val updatedSongs = _uiState.value.popularSongs.map { song ->
                    song.copy(isDownloaded = downloadedIds.contains(song.id))
                }
                _uiState.value = _uiState.value.copy(popularSongs = updatedSongs)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isOfflineMode = false)

            try {
                // Load all data in parallel
                val recentAlbumsResult = mediaRepository.getRecentlyAddedAlbums(limit = 10)
                val topArtistsResult = mediaRepository.getTopArtists(limit = 10)
                val playlistsResult = mediaRepository.getUserPlaylists(limit = 10)
                val popularSongsResult = mediaRepository.getPopularSongs(limit = 20)

                // Check if all results failed and device is offline
                val errors = listOfNotNull(
                    recentAlbumsResult.exceptionOrNull(),
                    topArtistsResult.exceptionOrNull(),
                    playlistsResult.exceptionOrNull(),
                    popularSongsResult.exceptionOrNull()
                )

                if (errors.size == 4 && !NetworkUtil.isConnected(context)) {
                    // Device is offline, load downloaded songs instead
                    loadOfflineData()
                    return@launch
                }

                // Update UI state with results
                _uiState.value = HomeUiState(
                    recentlyAddedAlbums = recentAlbumsResult.getOrElse { emptyList() },
                    topArtists = topArtistsResult.getOrElse { emptyList() },
                    playlists = playlistsResult.getOrElse { emptyList() },
                    popularSongs = popularSongsResult.getOrElse { emptyList() },
                    nowPlayingSong = null, // TODO: Implement now playing
                    isLoading = false,
                    isOfflineMode = false
                )

                if (errors.isNotEmpty()) {
                    _errorMessage.value = "Failed to load some content: ${errors.first().message}"
                }
            } catch (e: Exception) {
                // Check if offline and load downloaded songs
                if (!NetworkUtil.isConnected(context)) {
                    loadOfflineData()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _errorMessage.value = "Failed to load data: ${e.message}"
                }
            }
        }
    }

    private suspend fun loadOfflineData() {
        try {
            val downloadedEntities = offlineRepository.getAllDownloaded().first()
            val downloadedSongs = DownloadedSongMapper.toSongList(downloadedEntities)

            _uiState.value = HomeUiState(
                recentlyAddedAlbums = emptyList(),
                topArtists = emptyList(),
                playlists = emptyList(),
                popularSongs = emptyList(),
                nowPlayingSong = null,
                isLoading = false,
                isOfflineMode = true,
                downloadedSongs = downloadedSongs
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isOfflineMode = true,
                downloadedSongs = emptyList()
            )
            _errorMessage.value = "Offline mode: ${e.message}"
        }
    }

    fun retry() {
        _errorMessage.value = null
        _uiState.value = _uiState.value.copy(isOfflineMode = false, downloadedSongs = emptyList())
        loadData()
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            val result = mediaRepository.getUserPlaylists(limit = 10)
            result.onSuccess { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }
    }

    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album details
    }

    fun onArtistClick(artist: Artist) {
        onNavigateToArtistDetail(artist.id)
    }

    fun onSongClick(song: Song) {
        // Set queue with popular songs and start from the clicked song
        val popularSongs = _uiState.value.popularSongs
        val index = popularSongs.indexOf(song)
        if (index != -1 && popularSongs.isNotEmpty()) {
            playbackManager.setQueue(popularSongs, startIndex = index)
        } else {
            // Fallback to just playing the song
            playbackManager.playSong(song)
        }
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
        onNavigateToPlaylistDetail(playlist.id)
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
        onNavigateToSearch()
    }

    fun onProfileClick() {
        // Trigger logout
        onLogout()
    }

    fun onDownloadClick(song: Song) {
        viewModelScope.launch {
            try {
                // Get user's preferred download quality
                val quality = dataStoreManager.downloadQuality.first()
                // Trigger download
                offlineRepository.downloadSong(song, quality)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to download song: ${e.message}"
            }
        }
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
    val isLoading: Boolean = true,
    val isOfflineMode: Boolean = false,
    val downloadedSongs: List<Song> = emptyList()
)
