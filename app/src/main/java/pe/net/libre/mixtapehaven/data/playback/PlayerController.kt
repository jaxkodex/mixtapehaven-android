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

    private var controller: MediaController? = null
    private var pendingAction: (() -> Unit)? = null
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
        }
    }

    init {
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
        scope.launch {
            while (isActive) {
                controller?.let {
                    _positionMs.value = it.currentPosition.coerceAtLeast(0)
                    _durationMs.value = it.duration.coerceAtLeast(0)
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /** Replace the queue with [queue] and start playback at [startIndex]. */
    fun play(queue: List<Track>, startIndex: Int = 0) {
        val action = action@{
            val c = controller ?: return@action
            val items = queue.mapNotNull { track ->
                val id = track.id ?: return@mapNotNull null
                val url = repository.audioStreamUrl(id) ?: return@mapNotNull null
                tracksById[id] = track
                buildMediaItem(track, id, url)
            }
            if (items.isEmpty()) return@action
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
        if (controller != null) action() else pendingAction = action
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem() ?: Unit

    fun previous() = controller?.seekToPreviousMediaItem() ?: Unit

    fun seekToFraction(fraction: Float) {
        val c = controller ?: return
        val duration = c.duration
        if (duration > 0) c.seekTo((duration * fraction).toLong())
    }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        tracksById.clear()
        _nowPlaying.value = null
        _isPlaying.value = false
        _positionMs.value = 0
        _durationMs.value = 0
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
    }
}
