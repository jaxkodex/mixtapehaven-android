package pe.net.libre.mixtapehaven.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlaybackSource
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.data.playback.RandomWalk
import pe.net.libre.mixtapehaven.model.Album

class HomeViewModel(
    private val repository: JellyfinRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    data class UiState(
        val userName: String = "",
        val albums: List<Album> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
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
        viewModelScope.launch {
            runCatching { RandomWalk(repository, playerController).start() }
        }
    }
}
