package pe.net.libre.mixtapehaven.ui.screens.video

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
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

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        // handleAudioFocus ducks/pauses other apps' audio and pauses us for calls; without it the
        // raw player would play over Spotify etc. (musicController.pause() only covers our music).
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .build()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var candidates: List<String> = emptyList()
    private var candidateIndex = 0

    /** True once any candidate actually played; gates the STOPPED report in [onCleared]. */
    private var reachedReady = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) reachedReady = true
        }

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
            reportProgressLoop()
        }
    }

    /** Pause playback when the screen is no longer visible; there is no background video service. */
    fun onScreenStopped() {
        player.pause()
    }

    /**
     * Report progress every [PROGRESS_REPORT_MS] while playing, plus exactly one paused report per
     * pause (so the resume point lands) — then stay quiet to avoid indefinite idle network churn.
     */
    private suspend fun reportProgressLoop() {
        var reportedPause = false
        while (currentCoroutineContext().isActive) {
            delay(PROGRESS_REPORT_MS)
            val ready = player.playbackState == Player.STATE_READY
            when {
                ready && player.isPlaying -> {
                    reportedPause = false
                    reportProgress(paused = false)
                }
                ready && !reportedPause -> {
                    reportedPause = true
                    reportProgress(paused = true)
                }
            }
        }
    }

    private suspend fun reportProgress(paused: Boolean) {
        repository.reportVideoPlayback(
            itemId,
            player.currentPosition,
            VideoPlaybackEvent.PROGRESS,
            paused = paused,
            transcoding = candidateIndex > 0,
        )
    }

    private fun prepareCurrentCandidate(startMs: Long) {
        player.setMediaItem(MediaItem.fromUri(candidates[candidateIndex]), startMs)
        player.prepare()
    }

    override fun onCleared() {
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val playedSomething = reachedReady
        val transcoding = candidateIndex > 0
        player.removeListener(listener)
        player.release()
        // A session that never played must not report STOPPED: its position 0 would regress the
        // server-side resume point of a half-watched item.
        if (!playedSomething) return
        // viewModelScope is already cancelled here, so the final report (which fixes the server's
        // resume point) needs its own short-lived scope.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.reportVideoPlayback(
                itemId,
                positionMs,
                VideoPlaybackEvent.STOPPED,
                transcoding = transcoding,
            )
        }
    }

    private companion object {
        const val PROGRESS_REPORT_MS = 10_000L
    }
}
