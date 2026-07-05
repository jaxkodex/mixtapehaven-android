package pe.net.libre.mixtapehaven.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.diagnostics.DiagnosticsLog
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.toTrack
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlaybackSource
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.data.playback.RandomWalk
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track

class HomeViewModel(
    private val repository: JellyfinRepository,
    private val playerController: PlayerController,
    private val downloadManager: DownloadManager,
    private val diagnostics: DiagnosticsLog,
) : ViewModel() {

    data class UiState(
        val userName: String = "",
        val albums: List<Album> = emptyList(),
        val onDevice: List<Track> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _snackbarMessages = Channel<String>(Channel.BUFFERED)

    /** One-shot messages for transient UI feedback (e.g. a Snackbar), not persisted in [state]. */
    val snackbarMessages: Flow<String> = _snackbarMessages.receiveAsFlow()

    init {
        load()
        observeDownloads()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val userName = repository.session.first()?.userName.orEmpty()
            runCatching { repository.recentlyAddedAlbums() }.fold(
                onSuccess = { albums ->
                    _state.update { it.copy(userName = userName, albums = albums, loading = false) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            userName = userName,
                            loading = false,
                            error = error.message ?: "Could not load your library",
                        )
                    }
                },
            )
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
                    Log.w(TAG, "Random Walk failed to start", error)
                    diagnostics.log(TAG, "Random Walk failed: ${error.javaClass.simpleName}: ${error.message}")
                    _snackbarMessages.send("Couldn't start Random Walk: ${error.message ?: "unknown error"}")
                }
        }
    }

    /** Play the offline library starting from [track] (serves from local files first). */
    fun playOnDevice(track: Track) {
        val tracks = _state.value.onDevice
        val startIndex = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        if (tracks.isNotEmpty()) playerController.play(tracks, startIndex)
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
