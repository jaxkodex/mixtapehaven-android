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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.jellyfin.VideoPlaybackEvent
import pe.net.libre.mixtapehaven.data.network.ServerAvailability
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
    private val serverAvailability: ServerAvailability,
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

    /**
     * Bumped by every [startItem]. Work started for one item and finished after the next one began
     * checks this before touching shared state, so a slow answer cannot land on the wrong episode.
     */
    private var loadGeneration = 0

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _buffering.value = true
                Player.STATE_READY -> {
                    reachedReady = true
                    _buffering.value = false
                    // Bytes arriving over the network is proof the server answers, and it is free:
                    // folding it in keeps the fresh window warm for the whole session, so the next
                    // episode's gate can skip its ping instead of making every hop wait on one.
                    if (needsServer(candidates)) serverAvailability.report(success = true)
                }
                Player.STATE_ENDED -> {
                    _buffering.value = false
                    onPlaybackEnded()
                }
                // STATE_IDLE follows an error or a release; the error path below owns the flag there.
                else -> Unit
            }
        }

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
                // Every candidate exhausted on a server-backed stream usually means the server went
                // away mid-playback (VPN dropped, walked off the LAN). Confirm in the background and
                // swap in the message that names the real problem; the raw one stands until then.
                if (needsServer(candidates)) {
                    val generation = loadGeneration
                    viewModelScope.launch {
                        val unreachable = !serverAvailability.check()
                        // The check can outlive the item that failed: a tap on Up Next during it
                        // may already be playing a downloaded copy by now, and painting this
                        // message over that would accuse a working screen of being broken.
                        if (unreachable && generation == loadGeneration) {
                            _error.value = unreachableMessage(hasNetwork = serverAvailability.hasNetwork())
                        }
                    }
                }
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

    /**
     * Load [id], resume it, and begin playing. Used for the initial item and each autoplay hop.
     *
     * Owns the buffering flag for the whole attempt so the indicator cannot outlive it: [prepareItem]
     * clears it on the paths that end in an error, and a throw is caught here. Neither seam it calls
     * is fully guarded — [resolveSources] is injected, and the Room read inside `resolvePlayback` is
     * unguarded — and a spinner turning forever reads worse than the black frame it replaced.
     */
    private suspend fun startItem(id: String) {
        loadGeneration++
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
            // An item that will not resolve is usually the server, not the item: check before
            // blaming the title, so the LAN-only/VPN case is named for what it is.
            val reachable = serverAvailability.check()
            stopPlayback()
            fail(if (reachable) "Could not load this title" else unreachableMessage(serverAvailability.hasNetwork()))
            return
        }
        val sources = resolveSources(id)
        val blocker = playbackBlocker(sources)
        if (blocker != null) {
            // Nothing here will play, and on a user-driven hop the previous episode is still
            // playing right now. Stop it before [_nowPlaying] moves on: [reportProgressLoop] pairs
            // the player's playhead with whatever [_nowPlaying] holds, so leaving the old stream
            // running under the new id would save the old episode's position as the new one's
            // resume point — locally and on the server.
            stopPlayback()
            _nowPlaying.value = item
            fail(blocker)
            return
        }
        _nowPlaying.value = item
        candidates = sources
        candidateIndex = 0
        reachedReady = false
        prepareCurrentCandidate(startMs = resolved.positionMs)
        progressStore.record(item, resolved.positionMs, player.duration.durationOrZero(), VideoPlaybackEvent.STARTED)
        _upNext.value = loadUpNext(item)
    }

    /**
     * Why [sources] cannot play, or null when they can. Asked before any of it is published, so a
     * refusal leaves no half-switched state behind.
     *
     * The server check is what keeps the screen from sitting on a black frame while each candidate
     * fails its own connection attempt in turn. It is skipped entirely for a saved copy, so offline
     * playback of a download costs nothing.
     */
    private suspend fun playbackBlocker(sources: List<String>): String? = when {
        sources.isEmpty() -> "No playable source for this title"
        needsServer(sources) && !serverAvailability.check() ->
            unreachableMessage(hasNetwork = serverAvailability.hasNetwork())
        else -> null
    }

    /**
     * Wind the player down to nothing, so a load that ends in an error leaves no stream running
     * behind the message and no playhead for the progress reports to misattribute.
     */
    private fun stopPlayback() {
        player.stop()
        player.clearMediaItems()
        candidates = emptyList()
        candidateIndex = 0
        reachedReady = false
    }

    /** End the load with [message] on screen; the buffering indicator must not outlive it. */
    private fun fail(message: String) {
        _buffering.value = false
        _error.value = message
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

/**
 * Shown instead of a black frame when a title can only come from a server that isn't answering.
 * Having a network but no server is its own case — a LAN-only or VPN-gated server looks like a
 * broken app unless the message says what is actually wrong.
 */
internal fun unreachableMessage(hasNetwork: Boolean): String = if (hasNetwork) {
    "Can't reach your server. Check your network or VPN, or download this title to watch it offline."
} else {
    "Not available offline. Download this title to watch it without a connection."
}

/**
 * True when none of [candidates] can play without the server.
 *
 * A saved copy is handed to the player as a `file://` uri (see
 * [pe.net.libre.mixtapehaven.data.download.VideoDownloadManager.localUriFor]); every other
 * candidate is an http(s) stream.
 */
internal fun needsServer(candidates: List<String>): Boolean =
    candidates.none { it.startsWith("file:", ignoreCase = true) }

/** ExoPlayer reports an unknown duration as [C.TIME_UNSET]; treat that as "not known yet". */
private fun Long.durationOrZero(): Long = if (this == C.TIME_UNSET || this < 0L) 0L else this
