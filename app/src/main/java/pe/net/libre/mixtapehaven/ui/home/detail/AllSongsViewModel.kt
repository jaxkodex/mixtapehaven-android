package pe.net.libre.mixtapehaven.ui.home.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.DownloadedSongMapper
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkUtil
import pe.net.libre.mixtapehaven.ui.home.Song

data class AllSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val isOfflineMode: Boolean = false
)

class AllSongsViewModel(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
    private val offlineRepository: OfflineRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllSongsUiState())
    val uiState: StateFlow<AllSongsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                hasMorePages = true,
                isOfflineMode = false
            )

            mediaRepository.getAllSongs(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { songs ->
                    _uiState.value = _uiState.value.copy(
                        songs = songs,
                        isLoading = false,
                        errorMessage = null,
                        hasMorePages = songs.size >= PAGE_SIZE,
                        isOfflineMode = false
                    )
                },
                onFailure = { error ->
                    // Check if offline and load downloaded songs
                    if (!NetworkUtil.isConnected(context)) {
                        loadOfflineSongs()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load songs"
                        )
                    }
                }
            )
        }
    }

    private suspend fun loadOfflineSongs() {
        try {
            val downloadedEntities = offlineRepository.getAllDownloaded().first()
            val downloadedSongs = DownloadedSongMapper.toSongList(downloadedEntities)

            _uiState.value = _uiState.value.copy(
                songs = downloadedSongs,
                isLoading = false,
                errorMessage = null,
                hasMorePages = false,
                isOfflineMode = true
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Offline mode: ${e.message}",
                isOfflineMode = true
            )
        }
    }

    fun loadMore() {
        // Skip pagination in offline mode
        if (_uiState.value.isOfflineMode || _uiState.value.isLoadingMore || !_uiState.value.hasMorePages) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val currentSize = _uiState.value.songs.size

            mediaRepository.getAllSongs(limit = PAGE_SIZE, startIndex = currentSize).fold(
                onSuccess = { newSongs ->
                    _uiState.value = _uiState.value.copy(
                        songs = _uiState.value.songs + newSongs,
                        isLoadingMore = false,
                        hasMorePages = newSongs.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Failed to load more songs"
                    )
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                errorMessage = null,
                hasMorePages = true,
                isOfflineMode = false
            )

            mediaRepository.getAllSongs(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { songs ->
                    _uiState.value = _uiState.value.copy(
                        songs = songs,
                        isRefreshing = false,
                        errorMessage = null,
                        hasMorePages = songs.size >= PAGE_SIZE,
                        isOfflineMode = false
                    )
                },
                onFailure = { error ->
                    // Check if offline and load downloaded songs
                    if (!NetworkUtil.isConnected(context)) {
                        loadOfflineSongs()
                        _uiState.value = _uiState.value.copy(isRefreshing = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "Failed to refresh songs"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun onSongClick(song: Song) {
        // Set queue with all songs and start from the clicked song
        val allSongs = _uiState.value.songs
        val index = allSongs.indexOf(song)
        if (index != -1 && allSongs.isNotEmpty()) {
            playbackManager.setQueue(allSongs, startIndex = index)
        } else {
            // Fallback to just playing the song
            playbackManager.playSong(song)
        }
    }

    fun onPlayPauseClick() {
        playbackManager.togglePlayPause()
    }
}
