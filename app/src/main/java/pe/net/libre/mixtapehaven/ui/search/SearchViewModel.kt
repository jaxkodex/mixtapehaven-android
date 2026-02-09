package pe.net.libre.mixtapehaven.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.api.SearchHint
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.DownloadedSongMapper
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkConnectivityProvider
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.home.Artist
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.home.Song

/**
 * Search filter categories
 */
enum class SearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS
}

/**
 * UI State for search screen
 */
data class SearchUiState(
    val query: String = "",
    val hints: List<SearchHint> = emptyList(),
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val isOfflineMode: Boolean = false,
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the search screen
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val offlineRepository: OfflineRepository,
    private val playbackManager: PlaybackManager,
    private val networkProvider: NetworkConnectivityProvider,
    private val onNavigateToArtistDetail: (String) -> Unit = {},
    private val onNavigateToPlaylistDetail: (String) -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        // Setup debounced hints search
        _queryFlow
            .debounce(300)
            .filter { it.length >= 2 && !_uiState.value.hasSearched }
            .onEach { query ->
                searchHints(query)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Update search query
     */
    fun onQueryChange(query: String) {
        _queryFlow.value = query
        _uiState.value = _uiState.value.copy(
            query = query,
            hasSearched = false,
            hints = if (query.length < 2) emptyList() else _uiState.value.hints
        )
    }

    /**
     * Submit search query
     */
    fun onSearchSubmit() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            hasSearched = true,
            hints = emptyList(),
            errorMessage = null
        )

        viewModelScope.launch {
            if (!networkProvider.isConnected()) {
                // Offline mode: search only downloaded songs
                performOfflineSearch(query)
            } else {
                // Online mode: perform full search
                performOnlineSearch(query)
            }
        }
    }

    /**
     * Search hints for autocomplete
     */
    private suspend fun searchHints(query: String) {
        if (!networkProvider.isConnected()) return

        val result = mediaRepository.searchHints(query, limit = 10)
        result.onSuccess { hints ->
            _uiState.value = _uiState.value.copy(hints = hints)
        }
    }

    /**
     * Perform online search across all categories
     */
    private suspend fun performOnlineSearch(query: String) {
        try {
            // Launch 4 parallel search calls
            val songsDeferred = viewModelScope.async { mediaRepository.searchSongs(query) }
            val albumsDeferred = viewModelScope.async { mediaRepository.searchAlbums(query) }
            val artistsDeferred = viewModelScope.async { mediaRepository.searchArtists(query) }
            val playlistsDeferred = viewModelScope.async { mediaRepository.searchPlaylists(query) }

            // Wait for all results
            val results = awaitAll(songsDeferred, albumsDeferred, artistsDeferred, playlistsDeferred)

            val songsResult = results[0] as Result<List<Song>>
            val albumsResult = results[1] as Result<List<Album>>
            val artistsResult = results[2] as Result<List<Artist>>
            val playlistsResult = results[3] as Result<List<Playlist>>

            _uiState.value = _uiState.value.copy(
                songs = songsResult.getOrElse { emptyList() },
                albums = albumsResult.getOrElse { emptyList() },
                artists = artistsResult.getOrElse { emptyList() },
                playlists = playlistsResult.getOrElse { emptyList() },
                isLoading = false,
                isOfflineMode = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Search failed: ${e.message}"
            )
        }
    }

    /**
     * Perform offline search on downloaded songs only
     */
    private suspend fun performOfflineSearch(query: String) {
        try {
            val downloadedEntities = offlineRepository.searchDownloadedSongs(query)
            val songs = DownloadedSongMapper.toSongList(downloadedEntities)

            _uiState.value = _uiState.value.copy(
                songs = songs,
                albums = emptyList(),
                artists = emptyList(),
                playlists = emptyList(),
                isLoading = false,
                isOfflineMode = true
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Offline search failed: ${e.message}"
            )
        }
    }

    /**
     * Change active filter
     */
    fun onFilterChange(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    /**
     * Handle hint click
     */
    fun onHintClick(hint: SearchHint) {
        when (hint.type) {
            "Audio" -> {
                // Play song
                val song = Song(
                    id = hint.id,
                    title = hint.name,
                    artist = hint.artists?.firstOrNull()
                        ?: hint.albumArtist
                        ?: "Unknown Artist",
                    duration = formatDuration(hint.runTimeTicks),
                    albumCoverPlaceholder = getPlaceholderEmoji(hint.name)
                )
                playbackManager.playSong(song)
            }
            "MusicArtist" -> {
                onNavigateToArtistDetail(hint.id)
            }
            "Playlist" -> {
                onNavigateToPlaylistDetail(hint.id)
            }
            "MusicAlbum" -> {
                // TODO: Navigate to album detail
            }
        }
    }

    /**
     * Handle song click
     */
    fun onSongClick(song: Song) {
        // Set queue with current songs and play from selected song
        val currentSongs = when (_uiState.value.activeFilter) {
            SearchFilter.ALL, SearchFilter.SONGS -> _uiState.value.songs
            else -> listOf(song)
        }
        
        val index = currentSongs.indexOf(song)
        if (index != -1 && currentSongs.isNotEmpty()) {
            playbackManager.setQueue(currentSongs, startIndex = index)
        } else {
            playbackManager.playSong(song)
        }
    }

    /**
     * Handle album click
     */
    fun onAlbumClick(album: Album) {
        // TODO: Navigate to album detail
    }

    /**
     * Handle artist click
     */
    fun onArtistClick(artist: Artist) {
        onNavigateToArtistDetail(artist.id)
    }

    /**
     * Handle playlist click
     */
    fun onPlaylistClick(playlist: Playlist) {
        onNavigateToPlaylistDetail(playlist.id)
    }

    /**
     * Clear search
     */
    fun onClearSearch() {
        _queryFlow.value = ""
        _uiState.value = SearchUiState()
    }

    /**
     * Format runtime ticks to duration string
     */
    private fun formatDuration(ticks: Long?): String {
        if (ticks == null) return "0:00"
        val seconds = (ticks / 10_000_000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    /**
     * Generate placeholder emoji based on name
     */
    private fun getPlaceholderEmoji(name: String): String {
        val emojis = listOf("🎵", "🎶", "🎸", "🎹", "🎤", "🎧", "🎼", "🎺", "🎷", "🥁", "💿", "📻", "🎭", "🌟", "✨")
        return emojis[name.hashCode().mod(emojis.size)]
    }
}
