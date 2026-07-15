package pe.net.libre.mixtapehaven.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.playback.PlaybackSource
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.model.Track

class NowPlayingViewModel(
    private val playerController: PlayerController,
    downloadManager: DownloadManager,
) : ViewModel() {

    val nowPlaying: StateFlow<Track?> = playerController.nowPlaying
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val positionMs: StateFlow<Long> = playerController.positionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val source: StateFlow<PlaybackSource> = playerController.source
    val queue: StateFlow<List<Track>> = playerController.queue

    val upNext: StateFlow<Track?> = combine(
        playerController.queue,
        playerController.queueIndex,
    ) { queue, index ->
        queue.getOrNull(index + 1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val upNextOfflineReady: StateFlow<Boolean> =
        combine(upNext, downloadManager.downloads) { track, rows ->
            track?.id != null && rows.any { it.id == track.id && it.complete }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** Save percent (0-100) when the current track is being downloaded, else null. */
    val savingPercent: StateFlow<Int?> =
        combine(playerController.nowPlaying, downloadManager.progress) { track, progress ->
            if (progress != null && progress.track.id == track?.id) progress.percent else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** True when the current track is already saved for offline playback. */
    val offlineReady: StateFlow<Boolean> =
        combine(playerController.nowPlaying, downloadManager.downloads) { track, rows ->
            track?.id != null && rows.any { it.id == track.id && it.complete }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun playPause() = playerController.playPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekToFraction(fraction: Float) = playerController.seekToFraction(fraction)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
