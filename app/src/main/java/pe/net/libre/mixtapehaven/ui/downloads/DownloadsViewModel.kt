package pe.net.libre.mixtapehaven.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.DownloadedSongMapper
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository

class DownloadsViewModel(
    private val offlineRepository: OfflineRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _downloadedSongs = MutableStateFlow<List<DownloadedSongEntity>>(emptyList())
    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = _downloadedSongs.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        loadDownloadedSongs()
    }

    private fun loadDownloadedSongs() {
        viewModelScope.launch {
            offlineRepository.getAllDownloaded().collect { songs ->
                _downloadedSongs.value = sortSongs(songs, _sortOrder.value)
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _downloadedSongs.value = sortSongs(_downloadedSongs.value, order)
    }

    private fun sortSongs(songs: List<DownloadedSongEntity>, order: SortOrder): List<DownloadedSongEntity> {
        return when (order) {
            SortOrder.DATE_DESC -> songs.sortedByDescending { it.downloadDate }
            SortOrder.DATE_ASC -> songs.sortedBy { it.downloadDate }
            SortOrder.NAME_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOrder.SIZE_DESC -> songs.sortedByDescending { it.fileSize + it.imageSize }
            SortOrder.SIZE_ASC -> songs.sortedBy { it.fileSize + it.imageSize }
            SortOrder.LAST_PLAYED -> songs.sortedByDescending { it.lastAccessTime }
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            try {
                offlineRepository.deleteSong(songId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun playDownloadedSongs() {
        val songs = DownloadedSongMapper.toSongList(_downloadedSongs.value)

        if (songs.isNotEmpty()) {
            playbackManager.setQueue(songs, 0)
        }
    }

    fun playSong(entity: DownloadedSongEntity) {
        val song = DownloadedSongMapper.toSong(entity)
        playbackManager.playSong(song)
    }
}

enum class SortOrder(val displayName: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)"),
    LAST_PLAYED("Last Played")
}
