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
import pe.net.libre.mixtapehaven.data.playback.VideoProgressStore
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/**
 * Owns the video [player] (a separate ExoPlayer instance from the music stack) so playback
 * survives configuration changes. Pauses music on start, resumes from the last known position
 * (local or server, whichever is fresher), and records progress through [progressStore].
 *
 * Stream sources come from [resolveSources] as ordered candidates (direct play first, HLS
 * transcode fallback); a playback error falls through to the next candidate at the same position.
 * When an episode ends the next one in the season starts automatically.
 */
class VideoPlayerViewModel(
    context: Context,
    private val repository: JellyfinRepository,
    private val progressStore: VideoProgressStore,
    musicController: PlayerController,
    itemId: String,
    private val resolveSources: suspend (String) -> List<String>,
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

    /** The item currently on screen; changes when autoplay advances to the next episode. */
    private val _nowPlaying = MutableStateFlow<VideoItem?>(null)
    val nowPlaying: StateFlow<VideoItem?> = _nowPlaying.asStateFlow()

    /** The episode autoplay will roll into, if any — also drives the player's next button. */
    private val _upNext = MutableStateFlow<VideoItem?>(null)
    val upNext: StateFlow<VideoItem?> = _upNext.asStateFlow()

    private var candidates: List<String> = emptyList()
    private var candidateIndex = 0

    /** True once any candidate actually played; gates the STOPPED report in [onCleared]. */
    private var reachedReady = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> reachedReady = true
                Player.STATE_ENDED -> onPlaybackEnded()
                else -> Unit
            }
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
            startItem(itemId)
            reportProgressLoop()
        }
    }

    /** Load [id], resume it, and begin playing. Used for the initial item and each autoplay hop. */
    private suspend fun startItem(id: String) {
        // Cleared up front so a hop that fails below cannot leave the pill pointing at the
        // previous episode's successor, and so it never shows a stale value while loadUpNext runs.
        _upNext.value = null
        val resolved = progressStore.resolvePlayback(id)
        val item = resolved.item
        if (item == null) {
            _error.value = "Could not load this title"
            return
        }
        _nowPlaying.value = item
        candidates = resolveSources(id)
        candidateIndex = 0
        reachedReady = false
        if (candidates.isEmpty()) {
            _error.value = "No playable source for this title"
            return
        }
        _error.value = null
        prepareCurrentCandidate(startMs = resolved.positionMs)
        progressStore.record(item, resolved.positionMs, player.duration.durationOrZero(), VideoPlaybackEvent.STARTED)
        _upNext.value = loadUpNext(item)
    }

    /**
     * The episode following [item] in its season, or null for movies, the last episode, or when the
     * series listing is unreachable.
     *
     * Deliberately positional rather than the server's Next Up: autoplay should roll into the
     * literal next episode, whereas Next Up answers the different question of where to resume a
     * series (see [VideoDetailViewModel]).
     */
    private suspend fun loadUpNext(item: VideoItem): VideoItem? {
        val seriesId = item.seriesId?.takeIf { item.kind == VideoKind.EPISODE } ?: return null
        val episodes = runCatching { repository.seriesEpisodes(seriesId) }.getOrNull().orEmpty()
        val index = episodes.indexOfFirst { it.id == item.id }
        return if (index >= 0) episodes.getOrNull(index + 1) else null
    }

    /** Advance to the next episode, or leave the finished frame up when there is nothing to play. */
    private fun onPlaybackEnded() {
        val finished = _nowPlaying.value ?: return
        val next = _upNext.value
        viewModelScope.launch {
            // Finalize the finished item first so it leaves Continue watching before the next
            // episode's own STARTED report lands.
            progressStore.record(
                finished,
                player.duration.durationOrZero(),
                player.duration.durationOrZero(),
                VideoPlaybackEvent.STOPPED,
                transcoding = candidateIndex > 0,
            )
            if (next != null) startItem(next.id)
        }
    }

    /** Skip to the next episode on user request, finalizing the current one as partially watched. */
    fun playNext() {
        val next = _upNext.value ?: return
        val current = _nowPlaying.value
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val durationMs = player.duration.durationOrZero()
        viewModelScope.launch {
            if (current != null) {
                progressStore.record(
                    current,
                    positionMs,
                    durationMs,
                    VideoPlaybackEvent.STOPPED,
                    transcoding = candidateIndex > 0,
                )
            }
            startItem(next.id)
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
        val item = _nowPlaying.value ?: return
        progressStore.record(
            item,
            player.currentPosition.coerceAtLeast(0),
            player.duration.durationOrZero(),
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
        val item = _nowPlaying.value
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val durationMs = player.duration.durationOrZero()
        val playedSomething = reachedReady
        val transcoding = candidateIndex > 0
        player.removeListener(listener)
        player.release()
        // A session that never played must not report STOPPED: its position 0 would regress the
        // resume point of a half-watched item.
        if (!playedSomething || item == null) return
        // viewModelScope is already cancelled here, so the final write (which fixes the resume
        // point) needs its own short-lived scope.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            progressStore.record(
                item,
                positionMs,
                durationMs,
                VideoPlaybackEvent.STOPPED,
                transcoding = transcoding,
            )
        }
    }

    private companion object {
        const val PROGRESS_REPORT_MS = 10_000L
    }
}

/** ExoPlayer reports an unknown duration as [C.TIME_UNSET]; treat that as "not known yet". */
private fun Long.durationOrZero(): Long = if (this == C.TIME_UNSET || this < 0L) 0L else this
