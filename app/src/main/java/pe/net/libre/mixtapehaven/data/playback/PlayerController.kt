package pe.net.libre.mixtapehaven.data.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
import pe.net.libre.mixtapehaven.model.Track

/**
 * App-scoped wrapper around a media3 [MediaController] connected to [PlaybackService].
 * Exposes playback state as [StateFlow]s for the UI and resolves stream URLs via [repository].
 */
class PlayerController(
    context: Context,
    private val repository: JellyfinRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    /**
     * Resolves the playable URL for a [Track]. Defaults to the remote static stream from
     * [repository]; swap it to source local files (e.g. downloads) without touching [play].
     */
    var streamUrlResolver: (Track) -> String? = { it.id?.let(repository::audioStreamUrl) }

    /**
     * Invoked when playback nears the end of the queue (fewer than [REFILL_THRESHOLD] items
     * remain). The returned tracks are appended to the queue. Null disables refilling.
     */
    var onQueueRunningLow: (suspend () -> List<Track>)? = null

    private var controller: MediaController? = null
    private var pendingAction: (() -> Unit)? = null
    private var refilling = false
    private val tracksById = mutableMapOf<String, Track>()

    private val _nowPlaying = MutableStateFlow<Track?>(null)
    val nowPlaying: StateFlow<Track?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNowPlaying(mediaItem)
            maybeRefillQueue()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // Reflect seeks/transitions immediately, even while paused.
            _positionMs.value = newPosition.positionMs.coerceAtLeast(0)
        }
    }

    init {
        connect()
        scope.launch {
            while (isActive) {
                activeController()?.let { c ->
                    _durationMs.value = c.duration.coerceAtLeast(0)
                    // Position only advances while playing; seeks are handled by the listener.
                    if (c.isPlaying) _positionMs.value = c.currentPosition.coerceAtLeast(0)
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /** Replace the queue with [queue] and start playback at [startIndex]. */
    fun play(queue: List<Track>, startIndex: Int = 0) {
        val action = action@{
            val c = controller ?: return@action
            val items = buildItems(queue)
            if (items.isEmpty()) return@action
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
        val c = controller
        if (c != null && c.isConnected) {
            action()
        } else {
            // The service may have been destroyed, leaving a dead controller; rebuild it.
            pendingAction = action
            if (c != null) reconnect()
        }
    }

    fun playPause() {
        val c = activeController() ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = activeController()?.seekToNextMediaItem() ?: Unit

    fun previous() = activeController()?.seekToPreviousMediaItem() ?: Unit

    fun seekToFraction(fraction: Float) {
        val c = activeController() ?: return
        val duration = c.duration
        if (duration > 0) c.seekTo((duration * fraction).toLong())
    }

    fun stop() {
        activeController()?.run {
            stop()
            clearMediaItems()
        }
        tracksById.clear()
        _nowPlaying.value = null
        _isPlaying.value = false
        _positionMs.value = 0
        _durationMs.value = 0
    }

    /** The controller only if it is still connected to a live service, else null. */
    private fun activeController(): MediaController? = controller?.takeIf { it.isConnected }

    /**
     * Resolve [tracks] into media items, registering each in [tracksById] so [nowPlaying] can be
     * recovered for items appended later via the refill hook. Tracks without an id or a resolvable
     * URL are skipped.
     */
    private fun buildItems(tracks: List<Track>): List<MediaItem> =
        resolvePlayables(tracks, streamUrlResolver).map { (track, id, url) ->
            tracksById[id] = track
            buildMediaItem(track, id, url)
        }

    /** Ask [onQueueRunningLow] for more tracks and append them when the queue is running low. */
    private fun maybeRefillQueue() {
        val refill = onQueueRunningLow
        val c = activeController()
        if (refill == null || c == null || refilling) return
        if (!shouldRefillQueue(c.currentMediaItemIndex, c.mediaItemCount, REFILL_THRESHOLD)) return
        refilling = true
        scope.launch {
            try {
                val items = buildItems(refill())
                if (items.isNotEmpty()) activeController()?.addMediaItems(items)
            } finally {
                refilling = false
            }
        }
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                val c = future.get()
                controller = c
                c.addListener(listener)
                _isPlaying.value = c.isPlaying
                updateNowPlaying(c.currentMediaItem)
                pendingAction?.invoke()
                pendingAction = null
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun reconnect() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        connect()
    }

    private fun updateNowPlaying(mediaItem: MediaItem?) {
        _nowPlaying.value = mediaItem?.mediaId?.let { tracksById[it] }
    }

    private fun buildMediaItem(track: Track, id: String, url: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .apply { track.imageUrl?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
        const val REFILL_THRESHOLD = 3
    }
}

/** A [Track] paired with the stream URL used to build its media item. */
internal data class PlayableTrack(val track: Track, val id: String, val url: String)

/**
 * Pair each resolvable [Track] with its stream URL via [resolver]. Tracks without an id or whose
 * URL cannot be resolved are dropped. Pure (no media3/Android deps) so the resolver seam used by
 * [PlayerController.play] and the queue-refill hook is unit-coverable.
 */
internal fun resolvePlayables(
    tracks: List<Track>,
    resolver: (Track) -> String?,
): List<PlayableTrack> = tracks.mapNotNull { track ->
    val id = track.id ?: return@mapNotNull null
    val url = resolver(track) ?: return@mapNotNull null
    PlayableTrack(track, id, url)
}

/**
 * True when fewer than [threshold] items remain after [currentIndex] in a queue of [itemCount],
 * signalling that the refill hook should be invoked.
 */
internal fun shouldRefillQueue(currentIndex: Int, itemCount: Int, threshold: Int): Boolean =
    itemCount - currentIndex - 1 < threshold
