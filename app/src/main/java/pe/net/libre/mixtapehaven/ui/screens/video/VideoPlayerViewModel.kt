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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    /**
     * True whenever the surface has nothing to show: resolving a stream, filling the initial buffer,
     * rebuffering mid-playback, or re-preparing on the transcode fallback. Starts true because [init]
     * launches straight into [startItem].
     */
    private val _buffering = MutableStateFlow(true)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    /**
     * True while playback is live *or* about to be: playing, or rebuffering with the intent to
     * resume. False once paused, ended, or errored.
     *
     * Two things hang off this, and both are power decisions rather than display state. The screen
     * is held awake only while it is true, and [reportProgressLoop] only ticks while it is true.
     * Buffering counts as live deliberately — a two-second rebuffer must not blank the screen —
     * whereas [Player.isPlaying] would drop out for exactly that window.
     */
    private val _playbackActive = MutableStateFlow(false)
    val playbackActive: StateFlow<Boolean> = _playbackActive.asStateFlow()

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
            refreshPlaybackActive()
            when (playbackState) {
                Player.STATE_BUFFERING -> _buffering.value = true
                Player.STATE_READY -> {
                    reachedReady = true
                    _buffering.value = false
                }
                Player.STATE_ENDED -> {
                    _buffering.value = false
                    onPlaybackEnded()
                }
                // STATE_IDLE follows an error or a release; the error path below owns the flag there.
                else -> Unit
            }
        }

        // playWhenReady moves independently of the playback state — pausing mid-buffer changes only
        // this — so both callbacks have to recompute the flag.
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = refreshPlaybackActive()

        override fun onPlayerError(error: PlaybackException) {
            // Direct play failed (e.g. a codec the device can't decode): fall through to the
            // transcoded candidate, keeping the position already reached.
            val positionMs = player.currentPosition.coerceAtLeast(0)
            if (candidateIndex + 1 < candidates.size) {
                candidateIndex++
                // Keep the indicator up: the swap to the transcode is another wait, not a resume.
                _buffering.value = true
                prepareCurrentCandidate(startMs = positionMs)
            } else {
                _buffering.value = false
                _error.value = error.message ?: "Playback failed"
            }
        }
    }

    /**
     * True while the player holds a prepared item that has not finished: playing, paused, or
     * buffering. False before the first [prepareCurrentCandidate] and after an error (STATE_IDLE)
     * or the end of an item (STATE_ENDED).
     */
    private fun holdsLiveItem(): Boolean =
        player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING

    /** Recompute [playbackActive] from the player's current intent and state. */
    private fun refreshPlaybackActive() {
        _playbackActive.value = player.playWhenReady && holdsLiveItem()
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

    /**
     * Load [id], resume it, and begin playing. Used for the initial item and each autoplay hop.
     *
     * Owns the buffering flag for the whole attempt so the indicator cannot outlive it: [prepareItem]
     * clears it on the paths that end in an error, and a throw is caught here. Neither seam it calls
     * is fully guarded — [resolveSources] is injected, and the Room read inside `resolvePlayback` is
     * unguarded — and a spinner turning forever reads worse than the black frame it replaced.
     */
    private suspend fun startItem(id: String) {
        // Cleared up front so a hop that fails below cannot leave the pill pointing at the
        // previous episode's successor, and so it never shows a stale value while loadUpNext runs.
        _upNext.value = null
        // The previous item's error goes with it: the screen hides the indicator whenever an error
        // is up, so a stale one would leave the next hop looking frozen for the whole resolve
        // window rather than showing that work is under way.
        _error.value = null
        // Resolving the item and its sources are network hops with the player still idle, so the
        // flag is raised here rather than waiting for STATE_BUFFERING.
        _buffering.value = true
        val failure = runCatching { prepareItem(id) }.exceptionOrNull() ?: return
        // Cancellation is the scope shutting down, not a load failure; let it unwind.
        if (failure is CancellationException) throw failure
        _buffering.value = false
        _error.value = "Could not load this title"
    }

    /** The load proper; [startItem] wraps it so every exit clears the buffering flag. */
    private suspend fun prepareItem(id: String) {
        val resolved = progressStore.resolvePlayback(id)
        val item = resolved.item
        if (item == null) {
            _buffering.value = false
            _error.value = "Could not load this title"
            return
        }
        _nowPlaying.value = item
        candidates = resolveSources(id)
        candidateIndex = 0
        reachedReady = false
        if (candidates.isEmpty()) {
            _buffering.value = false
            _error.value = "No playable source for this title"
            return
        }
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
     * pause (so the resume point lands) — then stay quiet.
     *
     * Gated on [playbackActive] rather than looping unconditionally: the ViewModel outlives the
     * visible screen (backgrounding the app only pauses, it does not pop the back stack entry), so
     * an unconditional loop keeps waking the CPU every ten seconds for as long as the player is on
     * the stack — after a pause, after a terminal error, and for a video that ended with nothing to
     * play next. [collectLatest] suspends the loop outright in all of those.
     */
    private suspend fun reportProgressLoop() {
        _playbackActive.collectLatest { active ->
            if (!active) {
                // One report as playback stops, so the resume point lands.
                //
                // Guarded on the same state pair as the flag itself, not on STATE_READY alone.
                // Pausing during a rebuffer flips the flag from STATE_BUFFERING, and a READY-only
                // guard dropped that report for good: the state settling to READY afterwards
                // recomputes the flag to false, which conflates against the current false and
                // never re-enters this collector. STATE_IDLE (never prepared, or errored) and
                // STATE_ENDED stay excluded — those write their own STOPPED or show a message.
                if (holdsLiveItem()) reportProgress(paused = true)
                return@collectLatest
            }
            while (currentCoroutineContext().isActive) {
                delay(PROGRESS_REPORT_MS)
                // False during a mid-playback rebuffer, which keeps the flag up but has no new
                // position worth reporting.
                if (player.isPlaying) reportProgress(paused = false)
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
