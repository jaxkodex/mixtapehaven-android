package pe.net.libre.mixtapehaven.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.diagnostics.DiagnosticsLog
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.VideoDownloadManager
import pe.net.libre.mixtapehaven.data.download.toTrack
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.jellyfin.VideoLibrarySource
import pe.net.libre.mixtapehaven.data.network.ServerAvailability
import pe.net.libre.mixtapehaven.data.playback.PlaybackSource
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.data.playback.RandomWalk
import pe.net.libre.mixtapehaven.data.playback.VideoProgressStore
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.model.VideoItem

class HomeViewModel(
    private val repository: JellyfinRepository,
    private val videoLibrary: VideoLibrarySource,
    private val playerController: PlayerController,
    private val downloadManager: DownloadManager,
    private val videoDownloadManager: VideoDownloadManager,
    private val videoProgressStore: VideoProgressStore,
    private val diagnostics: DiagnosticsLog,
    private val serverAvailability: ServerAvailability,
) : ViewModel() {

    data class UiState(
        val userName: String = "",
        val albums: List<Album> = emptyList(),
        val videos: List<VideoItem> = emptyList(),
        /** Partially-watched movies/episodes for the Continue watching rail. */
        val continueWatching: List<VideoItem> = emptyList(),
        /** Ids with a completed offline copy, for the Continue card's download check. */
        val downloadedVideoIds: Set<String> = emptySet(),
        val onDevice: List<Track> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
        /**
         * Whether the media server can be reached — not merely whether the device has a network,
         * since a LAN-only or VPN-gated server is unreachable from a working mobile connection.
         * Starts optimistic so nothing is painted as unplayable before anything has failed.
         */
        val serverReachable: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Current track and playback state for the bottom Now Playing bar. */
    val nowPlaying: StateFlow<Track?> = playerController.nowPlaying
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying

    /** Last successful server Continue Watching fetch; empty until [load] completes or when offline. */
    private val serverContinueWatching = MutableStateFlow<List<VideoItem>>(emptyList())

    private val _snackbarMessages = Channel<String>(Channel.BUFFERED)

    /** One-shot messages for transient UI feedback (e.g. a Snackbar), not persisted in [state]. */
    val snackbarMessages: Flow<String> = _snackbarMessages.receiveAsFlow()

    init {
        load()
        observeDownloads()
        observeContinueWatching()
        observeServerAvailability()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val userName = repository.session.first()?.userName.orEmpty()
            // A server without video libraries (or a video fetch failure) must not break the
            // music Home, so videos degrade to an empty (hidden) section independently — and load
            // in parallel so the new section adds no latency to the album fetch.
            val videosDeferred = async {
                runCatching { videoLibrary.videoLibrary(limit = VIDEO_RAIL_LIMIT).items }
                    .getOrDefault(emptyList())
            }
            // Offline this fails and the rail falls back to the local table, so keep the last
            // known server list rather than clearing it.
            val continueDeferred = async { runCatching { repository.continueWatching() }.getOrNull() }
            val albumsResult = runCatching { repository.recentlyAddedAlbums() }
            val videos = videosDeferred.await()
            val serverContinue = continueDeferred.await()
            serverContinue?.let { serverContinueWatching.value = it }
            // These fetches double as a reachability probe — no extra request needed. All of them
            // failing is the signal that the server is not answering, whatever the reason (no
            // network, a LAN-only address from off the LAN, a VPN that is down).
            serverAvailability.report(albumsResult.isSuccess || serverContinue != null || videos.isNotEmpty())
            albumsResult.fold(
                onSuccess = { albums ->
                    _state.update { it.copy(userName = userName, albums = albums, videos = videos, loading = false) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            userName = userName,
                            videos = videos,
                            loading = false,
                            error = error.message ?: "Could not load your library",
                        )
                    }
                },
            )
        }
    }

    /**
     * Keep the Continue watching rail in sync. The local table is the base (it is the only source
     * that works offline); the server's list is layered on top when reachable so progress made on
     * other devices shows up too.
     */
    private fun observeContinueWatching() {
        viewModelScope.launch {
            combine(
                videoProgressStore.observeLocal(),
                serverContinueWatching,
                videoProgressStore.observeFinishedIds(),
                videoDownloadManager.downloads,
            ) { local, server, finishedIds, downloads ->
                mergeContinueWatching(local, server, finishedIds) to
                    downloads.filter { it.complete }.map { it.id }.toSet()
            }.collect { (merged, downloadedIds) ->
                _state.update { it.copy(continueWatching = merged, downloadedVideoIds = downloadedIds) }
            }
        }
    }

    /**
     * Track server reachability so the Continue watching rail can mark what it cannot play. With no
     * reachable server the rail still lists everything watched recently, but only downloaded titles
     * have bytes to play — showing them identically is what made a tap look like it did nothing.
     */
    private fun observeServerAvailability() {
        viewModelScope.launch {
            serverAvailability.reachable.collect { reachable ->
                _state.update { it.copy(serverReachable = reachable) }
            }
        }
    }

    /** Keep the "On your device" section in sync with the offline library. */
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadManager.downloads.collect { rows ->
                // Mapping rows -> Track is cheap but kept off Main to stay consistent with the DAO.
                val tracks = withContext(Dispatchers.IO) {
                    rows.filter { it.complete }.map { it.toTrack() }
                }
                _state.update { it.copy(onDevice = tracks) }
            }
        }
    }

    /** Load the album's tracks and start playback from the first one. */
    fun playAlbum(album: Album) {
        val albumId = album.id ?: return
        viewModelScope.launch {
            val tracks = runCatching { repository.albumTracks(albumId) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                playerController.setSource(PlaybackSource.LIBRARY)
                playerController.play(tracks, startIndex = 0)
            }
        }
    }

    /** Start an endless shuffled queue across the whole library. */
    fun startRandomWalk() {
        diagnostics.log(TAG, "Random Walk requested")
        viewModelScope.launch {
            runCatching { RandomWalk(repository, playerController).start() }
                .onSuccess { started ->
                    if (started) {
                        diagnostics.log(TAG, "Random Walk started")
                    } else {
                        diagnostics.log(TAG, "Random Walk produced no tracks (empty library or all filtered out)")
                        _snackbarMessages.send("Your library has no tracks to shuffle")
                    }
                }
                .onFailure { error ->
                    // Cancellation (scope cleared / navigated away) is not a failure: let it propagate
                    // so we don't log it or surface a spurious snackbar.
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Random Walk failed to start", error)
                    diagnostics.log(TAG, "Random Walk failed: ${error.javaClass.simpleName}: ${error.message}")
                    _snackbarMessages.send("Couldn't start Random Walk: ${error.message ?: "unknown error"}")
                }
        }
    }

    /**
     * Handle a tap on a Continue watching card: resume [video] through [onResume], or explain why
     * it cannot play when there is neither a reachable server nor a saved copy.
     *
     * Navigating anyway would land on the player's black frame while every stream candidate failed
     * in turn — the "nothing happened" this replaces.
     *
     * A downloaded title never waits on anything. Otherwise the dimmed state is only a belief, so
     * it is confirmed with [ServerAvailability.check] before refusing: a server that came back
     * (VPN reconnected) must play on the first tap, not after a manual reload.
     */
    fun resumeVideo(video: VideoItem, onResume: (String) -> Unit) {
        val state = _state.value
        if (canPlayNow(video.id, state.serverReachable, state.downloadedVideoIds)) {
            onResume(video.id)
            return
        }
        viewModelScope.launch {
            if (serverAvailability.check()) {
                onResume(video.id)
                return@launch
            }
            diagnostics.log(TAG, "Blocked resume of ${video.id}: server unreachable, no saved copy")
            _snackbarMessages.send(unavailableMessage(video.title, hasNetwork = serverAvailability.hasNetwork()))
        }
    }

    fun playPause() = playerController.playPause()

    fun playNext() = playerController.next()

    /** Play the offline library starting from [track] (serves from local files first). */
    fun playOnDevice(track: Track) {
        val tracks = _state.value.onDevice
        val startIndex = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        if (tracks.isNotEmpty()) playerController.play(tracks, startIndex)
    }

    private companion object {
        const val TAG = "HomeViewModel"

        /** Titles in the Home "Movies & shows" rail; the full set lives behind "See all". */
        const val VIDEO_RAIL_LIMIT = 20
    }
}

/** How many titles the Continue watching rail shows. */
private const val CONTINUE_WATCHING_LIMIT = 12

/**
 * Whether tapping the Continue watching card for [id] can start playback without further checks:
 * with the server reachable anything streams, without it only a completed download
 * ([downloadedIds]) has bytes to play.
 */
internal fun canPlayNow(
    id: String,
    serverReachable: Boolean,
    downloadedIds: Set<String>,
): Boolean = serverReachable || id in downloadedIds

/**
 * Why [title] cannot play. The two cases need different words: with no network the user knows what
 * to do, whereas "you're online but your server isn't answering" is the LAN-only/VPN case they
 * would otherwise read as the app being broken.
 */
internal fun unavailableMessage(title: String, hasNetwork: Boolean): String = if (hasNetwork) {
    "$title isn't downloaded, and your server isn't reachable — check your VPN or network"
} else {
    "$title isn't downloaded — connect to watch it"
}

/**
 * Merge the [local] and [server] Continue watching lists into one rail, most recently watched
 * first.
 *
 * An id in both sides is kept once, taking whichever record was written later — the server knows
 * about other devices, the local table knows about offline viewing, and neither is reliably ahead.
 *
 * [finishedIds] are titles finished on this device. They are removed outright because the server
 * snapshot is only refetched by a reload: without this, the episode just watched to the end would
 * reappear in the rail as soon as its local row stopped counting as in-progress.
 */
internal fun mergeContinueWatching(
    local: List<VideoItem>,
    server: List<VideoItem>,
    finishedIds: Set<String> = emptySet(),
): List<VideoItem> = (local + server)
    .filterNot { it.id in finishedIds }
    .groupBy { it.id }
    .mapNotNull { (_, records) -> records.maxByOrNull { it.lastPlayedAtMs } }
    .filter { it.resumePositionMs > 0 }
    .sortedByDescending { it.lastPlayedAtMs }
    .take(CONTINUE_WATCHING_LIMIT)
