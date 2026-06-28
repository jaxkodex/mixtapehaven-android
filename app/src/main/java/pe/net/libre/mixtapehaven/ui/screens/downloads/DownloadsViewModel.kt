package pe.net.libre.mixtapehaven.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadProgress
import pe.net.libre.mixtapehaven.data.download.formatBytes
import pe.net.libre.mixtapehaven.data.download.toTrack
import pe.net.libre.mixtapehaven.model.Track

/** Backs the Downloads screen with the real saved library, in-flight progress, and storage totals. */
class DownloadsViewModel(
    private val downloadManager: DownloadManager,
) : ViewModel() {

    data class UiState(
        val downloading: DownloadProgress? = null,
        val saved: List<Track> = emptyList(),
        val totalLabel: String = formatBytes(0),
        val usedFraction: Float = 0f,
    )

    val state: StateFlow<UiState> =
        combine(downloadManager.downloads, downloadManager.progress) { rows, progress ->
            val completed = rows.filter { it.complete }
            val totalBytes = completed.sumOf { it.sizeBytes }
            UiState(
                downloading = progress,
                saved = completed.map { it.toTrack() },
                totalLabel = formatBytes(totalBytes),
                usedFraction = usedFractionOf(totalBytes, downloadManager.usableSpaceBytes()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState())

    fun removeAll() {
        viewModelScope.launch { downloadManager.removeAll() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Fraction of the storage bar to fill: used bytes over (used + free), clamped to a sane minimum. */
internal fun usedFractionOf(usedBytes: Long, freeBytes: Long): Float {
    val capacity = usedBytes + freeBytes
    if (capacity <= 0L || usedBytes <= 0L) return 0f
    return (usedBytes.toFloat() / capacity).coerceIn(0f, 1f)
}
