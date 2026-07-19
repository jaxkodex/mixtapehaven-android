package pe.net.libre.mixtapehaven.ui.screens.downloads

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadProgress
import pe.net.libre.mixtapehaven.data.download.DownloadSettingsStore
import pe.net.libre.mixtapehaven.data.download.DownloadedTrack
import pe.net.libre.mixtapehaven.data.download.DownloadedVideo
import pe.net.libre.mixtapehaven.data.download.VideoDownloadManager
import pe.net.libre.mixtapehaven.data.download.VideoDownloadProgress
import pe.net.libre.mixtapehaven.data.download.VideoDownloadStatus
import pe.net.libre.mixtapehaven.data.download.formatBytes
import pe.net.libre.mixtapehaven.data.download.toTrack
import pe.net.libre.mixtapehaven.model.Track

/** One saved or in-flight video download rendered on the Downloads screen. */
data class SavedVideoUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val sizeLabel: String,
    val posterUrl: String?,
    val artColor: Color,
    val status: VideoDownloadStatus,
)

/** Backs the Downloads screen with the saved audio + video libraries, progress, and storage totals. */
class DownloadsViewModel(
    private val downloadManager: DownloadManager,
    private val videoDownloadManager: VideoDownloadManager,
    settingsStore: DownloadSettingsStore,
) : ViewModel() {

    data class UiState(
        val downloading: DownloadProgress? = null,
        val saved: List<Track> = emptyList(),
        val videos: List<SavedVideoUi> = emptyList(),
        val totalLabel: String = formatBytes(0),
        val audioTotalLabel: String = formatBytes(0),
        val videoTotalLabel: String = formatBytes(0),
        val audioFraction: Float = 0f,
        val videoFraction: Float = 0f,
    )

    val state: StateFlow<UiState> =
        combine(
            downloadManager.downloads,
            downloadManager.progress,
            videoDownloadManager.downloads,
            videoDownloadManager.progress,
            settingsStore.wifiOnly,
        ) { rows, progress, videoRows, videoProgress, wifiOnly ->
            // Mapping + File.usableSpace touch disk; keep them off the Main thread.
            withContext(Dispatchers.IO) {
                buildState(rows.filter { it.complete }, progress, videoRows, videoProgress, wifiOnly)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState())

    private fun buildState(
        completedTracks: List<DownloadedTrack>,
        progress: DownloadProgress?,
        videoRows: List<DownloadedVideo>,
        videoProgress: Map<String, VideoDownloadProgress>,
        wifiOnly: Boolean,
    ): UiState {
        val audioBytes = completedTracks.sumOf { it.sizeBytes }
        val videoBytes = videoRows.filter { it.complete }.sumOf { it.sizeBytes }
        val free = videoDownloadManager.usableSpaceBytes()
        // The queue is only ever gated on network when the Wi-Fi-only constraint is set and the
        // current network would not satisfy it — that's when "Waiting…" deserves the reason.
        val waitingLabel =
            if (wifiOnly && videoDownloadManager.isNetworkMetered()) "Waiting for Wi-Fi" else "Waiting…"
        return UiState(
            downloading = progress,
            saved = completedTracks.map { it.toTrack() },
            videos = videoRows.map { it.toSavedVideoUi(videoProgress, waitingLabel) },
            totalLabel = formatBytes(audioBytes + videoBytes),
            audioTotalLabel = formatBytes(audioBytes),
            videoTotalLabel = formatBytes(videoBytes),
            audioFraction = usedFractionOf(audioBytes, videoBytes + free),
            videoFraction = usedFractionOf(videoBytes, audioBytes + free),
        )
    }

    /** Cancels an in-flight video download or deletes the saved copy. */
    fun removeVideo(id: String) {
        viewModelScope.launch { videoDownloadManager.remove(id) }
    }

    /** Re-queues a failed video download. */
    fun retryVideo(id: String) {
        videoDownloadManager.retry(id)
    }

    fun removeAll() {
        viewModelScope.launch {
            downloadManager.removeAll()
            videoDownloadManager.removeAll()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Map a download row to its screen representation; in-flight rows show streamed bytes so far. */
private fun DownloadedVideo.toSavedVideoUi(
    progress: Map<String, VideoDownloadProgress>,
    waitingLabel: String,
): SavedVideoUi {
    val status = VideoDownloadStatus.fromName(status)
    return SavedVideoUi(
        id = id,
        title = title,
        subtitle = listOfNotNull(seriesName, seasonEpisodeLabel, qualityLabel).joinToString(" · "),
        sizeLabel = when (status) {
            VideoDownloadStatus.COMPLETE -> formatBytes(sizeBytes)
            VideoDownloadStatus.RUNNING -> formatBytes(progress[id]?.bytes ?: 0)
            VideoDownloadStatus.QUEUED -> waitingLabel
            VideoDownloadStatus.FAILED -> "Download failed"
        },
        posterUrl = posterUrl,
        artColor = Color(artColorArgb),
        status = status,
    )
}

/** Fraction of the storage bar to fill: used bytes over (used + free), clamped to a sane minimum. */
internal fun usedFractionOf(usedBytes: Long, freeBytes: Long): Float {
    val capacity = usedBytes + freeBytes
    if (capacity <= 0L || usedBytes <= 0L) return 0f
    return (usedBytes.toFloat() / capacity).coerceIn(0f, 1f)
}
