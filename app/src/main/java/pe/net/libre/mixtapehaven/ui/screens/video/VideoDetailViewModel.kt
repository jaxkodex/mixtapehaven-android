package pe.net.libre.mixtapehaven.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.download.VideoDownloadManager
import pe.net.libre.mixtapehaven.data.download.formatBytes
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/**
 * Per-item download state shown on the detail screen: [downloadedIds] have a saved offline copy;
 * [inFlightLabels] maps an actively downloading id to its progress label (e.g. "142 MB").
 */
data class VideoDownloadUi(
    val downloadedIds: Set<String> = emptySet(),
    val inFlightLabels: Map<String, String> = emptyMap(),
)

class VideoDetailViewModel(
    private val repository: JellyfinRepository,
    private val downloadManager: VideoDownloadManager,
    private val itemId: String,
) : ViewModel() {

    data class UiState(
        val item: VideoItem? = null,
        val episodes: List<VideoItem> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val downloadUi: StateFlow<VideoDownloadUi> =
        combine(downloadManager.downloads, downloadManager.progress) { rows, progress ->
            VideoDownloadUi(
                downloadedIds = rows.filter { it.complete }.map { it.id }.toSet(),
                inFlightLabels = progress.mapValues { (_, p) -> formatBytes(p.bytes) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), VideoDownloadUi())

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val item = requireNotNull(repository.videoItem(itemId)) { "Item not found" }
                val episodes = if (item.kind == VideoKind.SERIES) repository.seriesEpisodes(itemId) else emptyList()
                item to episodes
            }.fold(
                onSuccess = { (item, episodes) ->
                    _state.update { it.copy(item = item, episodes = episodes, loading = false) }
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Could not load this title") }
                },
            )
        }
    }

    /** See [selectPlayTarget]. Null while loading or on error. */
    fun playTarget(): VideoItem? = selectPlayTarget(_state.value.item, _state.value.episodes)

    fun download(item: VideoItem) = downloadManager.download(item)

    /** Cancels an in-flight download or deletes the saved copy of [id]. */
    fun removeDownload(id: String) {
        viewModelScope.launch { downloadManager.remove(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * The item playback should start with: the movie/episode itself, or for a series the first
 * in-progress episode, falling back to the first episode.
 *
 * Known limitation: once an episode is watched to completion Jellyfin zeroes its position, so a
 * user who finished E1 (with nothing in progress) is sent to E1 again instead of E2. Switching to
 * tvShowsApi.getNextUp would solve exactly this.
 */
internal fun selectPlayTarget(item: VideoItem?, episodes: List<VideoItem>): VideoItem? = when {
    item == null -> null
    item.kind != VideoKind.SERIES -> item
    else -> episodes.firstOrNull { it.resumePositionMs > 0 } ?: episodes.firstOrNull()
}
