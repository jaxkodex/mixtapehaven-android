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
        /** For a series, the server's Next Up episode — what the Play button starts. */
        val nextUp: VideoItem? = null,
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** One human-readable message per failed download; the screen toasts these. */
    val downloadErrors = downloadManager.errors

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
                val series = item.kind == VideoKind.SERIES
                val episodes = if (series) repository.seriesEpisodes(itemId) else emptyList()
                // Next Up is advisory: a server that cannot answer falls back to the episode scan
                // in selectPlayTarget rather than failing the whole screen.
                val nextUp = if (series) runCatching { repository.nextUpEpisode(itemId) }.getOrNull() else null
                Triple(item, episodes, nextUp)
            }.fold(
                onSuccess = { (item, episodes, nextUp) ->
                    _state.update {
                        it.copy(item = item, episodes = episodes, nextUp = nextUp, loading = false)
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Could not load this title") }
                },
            )
        }
    }

    /** See [selectPlayTarget]. Null while loading or on error. */
    fun playTarget(): VideoItem? =
        selectPlayTarget(_state.value.item, _state.value.episodes, _state.value.nextUp)

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
 * The item playback should start with: the movie/episode itself, or for a series the server's
 * [nextUp] episode.
 *
 * Next Up is what makes "finished E1, press Play" land on E2: Jellyfin zeroes an episode's
 * position once it is watched, so the local fallback below — first in-progress episode, else the
 * first episode — would send that user back to E1. The fallback only runs when the server could
 * not answer (offline, or an older server), where being off by one beats not playing at all.
 */
internal fun selectPlayTarget(
    item: VideoItem?,
    episodes: List<VideoItem>,
    nextUp: VideoItem? = null,
): VideoItem? = when {
    item == null -> null
    item.kind != VideoKind.SERIES -> item
    nextUp != null -> nextUp
    else -> episodes.firstOrNull { it.resumePositionMs > 0 } ?: episodes.firstOrNull()
}
