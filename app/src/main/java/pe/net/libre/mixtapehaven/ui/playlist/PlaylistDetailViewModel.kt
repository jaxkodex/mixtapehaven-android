package pe.net.libre.mixtapehaven.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkUtil
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.home.Song

data class PlaylistDownloadState(
    val status: PlaylistDownloadStatus? = null,
    val downloadedSongs: Int = 0,
    val totalSongs: Int = 0,
    val isCheckingNetwork: Boolean = false,
    val showMobileDataDialog: Boolean = false
)

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMix: Boolean = false,
    val errorMessage: String? = null,
    val totalDuration: String = "0 hr 0 min",
    val downloadState: PlaylistDownloadState = PlaylistDownloadState()
)

class PlaylistDetailViewModel(
    private val playlistId: String,
    private val mediaRepository: MediaRepository,
    private val offlineRepository: OfflineRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlaylistData()
        loadDownloadState()
    }

    private fun loadDownloadState() {
        viewModelScope.launch {
            val downloadState = offlineRepository.getPlaylistDownloadState(playlistId)
            downloadState?.let { state ->
                _uiState.value = _uiState.value.copy(
                    downloadState = PlaylistDownloadState(
                        status = state.status,
                        downloadedSongs = state.downloadedSongs,
                        totalSongs = state.totalSongs
                    )
                )
            }
        }
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
            _uiState.update { it.copy(isLoadingMix = true, errorMessage = null) }
            try {
                mediaRepository.getPlaylistInstantMix(playlistId)
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

    fun onDownloadClick(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadState = _uiState.value.downloadState.copy(isCheckingNetwork = true)
            )

            // Check if on mobile data
            val networkType = NetworkUtil.getNetworkType(context)
            val wifiOnly = true // Should come from settings

            if (networkType == NetworkUtil.NetworkType.CELLULAR && wifiOnly) {
                // Check if we have a stored preference for this playlist
                val allowMobileData = offlineRepository.shouldAllowMobileDataForPlaylist(playlistId)

                if (!allowMobileData) {
                    _uiState.value = _uiState.value.copy(
                        downloadState = _uiState.value.downloadState.copy(
                            isCheckingNetwork = false,
                            showMobileDataDialog = true
                        )
                    )
                    return@launch
                }
            }

            startDownload()
        }
    }

    fun onMobileDataDialogConfirm(rememberChoice: Boolean) {
        viewModelScope.launch {
            if (rememberChoice) {
                offlineRepository.setMobileDataPreference(playlistId, true)
            }

            _uiState.value = _uiState.value.copy(
                downloadState = _uiState.value.downloadState.copy(showMobileDataDialog = false)
            )

            startDownload()
        }
    }

    fun onMobileDataDialogDismiss() {
        _uiState.value = _uiState.value.copy(
            downloadState = _uiState.value.downloadState.copy(showMobileDataDialog = false)
        )
    }

    private fun startDownload() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadState = _uiState.value.downloadState.copy(isCheckingNetwork = false)
            )

            // Fetch playlist with sizes
            val result = mediaRepository.getPlaylistItemsWithSizes(playlistId)

            result.fold(
                onSuccess = { playlistWithSongs ->
                    offlineRepository.downloadPlaylist(playlistWithSongs, "ORIGINAL")

                    // Update UI state
                    _uiState.value = _uiState.value.copy(
                        downloadState = PlaylistDownloadState(
                            status = PlaylistDownloadStatus.DOWNLOADING,
                            downloadedSongs = 0,
                            totalSongs = playlistWithSongs.songs.size
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to start download: ${error.message}"
                    )
                }
            )
        }
    }

    fun onPauseDownloadClick() {
        viewModelScope.launch {
            offlineRepository.pausePlaylistDownload(playlistId)
            _uiState.value = _uiState.value.copy(
                downloadState = _uiState.value.downloadState.copy(
                    status = PlaylistDownloadStatus.PAUSED
                )
            )
        }
    }

    fun onResumeDownloadClick() {
        viewModelScope.launch {
            offlineRepository.resumePlaylistDownload(playlistId)
            _uiState.value = _uiState.value.copy(
                downloadState = _uiState.value.downloadState.copy(
                    status = PlaylistDownloadStatus.DOWNLOADING
                )
            )
        }
    }

    fun onCancelDownloadClick() {
        viewModelScope.launch {
            offlineRepository.cancelPlaylistDownload(playlistId)
            _uiState.value = _uiState.value.copy(
                downloadState = PlaylistDownloadState()
            )
        }
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
