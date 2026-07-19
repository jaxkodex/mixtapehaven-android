package pe.net.libre.mixtapehaven.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.model.VideoItem

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: JellyfinRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val results: List<Track> = emptyList(),
        val videos: List<VideoItem> = emptyList(),
        val loading: Boolean = false,
    ) {
        val isEmpty: Boolean get() = results.isEmpty() && videos.isEmpty()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .map { it.trim() }
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { query -> runSearch(query) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    fun playFrom(index: Int) {
        val results = _state.value.results
        if (index in results.indices) playerController.play(results, startIndex = index)
    }

    fun playAll() {
        val results = _state.value.results
        if (results.isNotEmpty()) playerController.play(results, startIndex = 0)
    }

    /**
     * Songs and video are searched concurrently: they are independent round trips, and a library
     * with no video at all should not make every search wait on a second sequential request.
     */
    private suspend fun runSearch(query: String) = coroutineScope {
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), videos = emptyList(), loading = false) }
            return@coroutineScope
        }
        _state.update { it.copy(loading = true) }
        val tracks = async { runCatching { repository.search(query) }.getOrDefault(emptyList()) }
        val videos = async { runCatching { repository.searchVideos(query) }.getOrDefault(emptyList()) }
        _state.update { it.copy(results = tracks.await(), videos = videos.await(), loading = false) }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
    }
}
