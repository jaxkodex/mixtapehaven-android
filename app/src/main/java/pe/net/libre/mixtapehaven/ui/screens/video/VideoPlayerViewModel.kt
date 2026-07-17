package pe.net.libre.mixtapehaven.ui.screens.video

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.jellyfin.VideoPlaybackEvent
import pe.net.libre.mixtapehaven.data.playback.PlayerController

/**
 * Owns the video [player] (a separate ExoPlayer instance from the music stack) so playback
 * survives configuration changes. Pauses music on start, resumes from the server-side position,
 * and reports start/progress/stopped back to Jellyfin so Continue Watching stays in sync.
 *
 * Stream sources come from [resolveSources] as ordered candidates (direct play first, HLS
 * transcode fallback); a playback error falls through to the next candidate at the same position.
 */
class VideoPlayerViewModel(
    context: Context,
    private val repository: JellyfinRepository,
    musicController: PlayerController,
    private val itemId: String,
    private val resolveSources: (String) -> List<String>,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var candidates: List<String> = emptyList()
    private var candidateIndex = 0

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            // Direct play failed (e.g. a codec the device can't decode): fall through to the
            // transcoded candidate, keeping the position already reached.
            val positionMs = player.currentPosition.coerceAtLeast(0)
            if (candidateIndex + 1 < candidates.size) {
                candidateIndex++
                prepareCurrentCandidate(startMs = positionMs)
            } else {
                _error.value = error.message ?: "Playback failed"
            }
        }
    }

    init {
        // One audio stream at a time: the video player takes over, music keeps its queue.
        musicController.pause()
        player.addListener(listener)
        player.playWhenReady = true
        viewModelScope.launch {
            val resumeMs = runCatching { repository.videoItem(itemId) }
                .getOrNull()?.resumePositionMs ?: 0L
            candidates = resolveSources(itemId)
            if (candidates.isEmpty()) {
                _error.value = "No playable source for this title"
                return@launch
            }
            prepareCurrentCandidate(startMs = resumeMs)
            repository.reportVideoPlayback(itemId, resumeMs, VideoPlaybackEvent.STARTED)
            while (isActive) {
                delay(PROGRESS_REPORT_MS)
                if (player.playbackState == Player.STATE_READY) {
                    repository.reportVideoPlayback(
                        itemId,
                        player.currentPosition,
                        VideoPlaybackEvent.PROGRESS,
                        paused = !player.isPlaying,
                    )
                }
            }
        }
    }

    private fun prepareCurrentCandidate(startMs: Long) {
        player.setMediaItem(MediaItem.fromUri(candidates[candidateIndex]), startMs)
        player.prepare()
    }

    override fun onCleared() {
        val positionMs = player.currentPosition.coerceAtLeast(0)
        player.removeListener(listener)
        player.release()
        // viewModelScope is already cancelled here, so the final report (which fixes the server's
        // resume point) needs its own short-lived scope.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.reportVideoPlayback(itemId, positionMs, VideoPlaybackEvent.STOPPED)
        }
    }

    private companion object {
        const val PROGRESS_REPORT_MS = 10_000L
    }
}
