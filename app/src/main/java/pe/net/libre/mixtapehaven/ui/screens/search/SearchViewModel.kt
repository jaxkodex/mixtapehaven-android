package pe.net.libre.mixtapehaven.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.model.Track

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: JellyfinRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val results: List<Track> = emptyList(),
        val loading: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .map { it.trim() }
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> runSearch(query) }
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

    private suspend fun runSearch(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), loading = false) }
            return
        }
        _state.update { it.copy(loading = true) }
        val results = runCatching { repository.search(query) }.getOrDefault(emptyList())
        _state.update { it.copy(results = results, loading = false) }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
    }
}
