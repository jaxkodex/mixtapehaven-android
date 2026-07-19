package pe.net.libre.mixtapehaven.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.jellyfin.VideoFilter
import pe.net.libre.mixtapehaven.data.jellyfin.VideoSort
import pe.net.libre.mixtapehaven.model.VideoItem

/**
 * Backs the paged video library grid: kind/genre/sort selection plus offset paging.
 *
 * Changing any facet restarts paging from zero rather than filtering what is already loaded — the
 * loaded list is only ever a prefix of the server-side result set, so filtering it locally would
 * silently hide everything past the first page.
 */
class VideoLibraryViewModel(
    private val repository: JellyfinRepository,
) : ViewModel() {

    data class UiState(
        val items: List<VideoItem> = emptyList(),
        val genres: List<String> = emptyList(),
        val filter: VideoFilter = VideoFilter.ALL,
        val genre: String? = null,
        val sort: VideoSort = VideoSort.RECENTLY_ADDED,
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: String? = null,
        /** Size of the whole filtered set on the server, not of [items]. */
        val totalCount: Int = 0,
    ) {
        /** True once the loaded prefix covers the whole filtered set. */
        val endReached: Boolean get() = items.size >= totalCount
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** The in-flight page fetch, cancelled when a facet change makes its result irrelevant. */
    private var pageJob: Job? = null

    init {
        reload()
        viewModelScope.launch {
            val genres = runCatching { repository.videoGenres() }.getOrDefault(emptyList())
            _state.update { it.copy(genres = genres) }
        }
    }

    fun setFilter(filter: VideoFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        reload()
    }

    /** Passing the already-selected genre clears it, so the chips toggle. */
    fun setGenre(genre: String?) {
        val next = if (genre == _state.value.genre) null else genre
        if (next == _state.value.genre) return
        _state.update { it.copy(genre = next) }
        reload()
    }

    fun setSort(sort: VideoSort) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        reload()
    }

    fun retry() = reload()

    /** Fetch the next page. No-op while a fetch is in flight or the set is exhausted. */
    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.endReached) return
        _state.update { it.copy(loadingMore = true) }
        fetch(startIndex = current.items.size, append = true)
    }

    private fun reload() {
        _state.update { it.copy(loading = true, loadingMore = false, error = null) }
        fetch(startIndex = 0, append = false)
    }

    private fun fetch(startIndex: Int, append: Boolean) {
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            val facets = _state.value
            val page = runCatching {
                repository.videoLibrary(
                    filter = facets.filter,
                    genre = facets.genre,
                    sort = facets.sort,
                    startIndex = startIndex,
                )
            }
            _state.update { state ->
                page.fold(
                    onSuccess = { result ->
                        val items = if (append) state.items + result.items else result.items
                        state.copy(
                            items = items,
                            loading = false,
                            loadingMore = false,
                            error = null,
                            totalCount = result.totalCount,
                        )
                    },
                    onFailure = { error ->
                        // A failed "load more" keeps what is already on screen; only a fresh load
                        // has nothing to fall back to and needs the error state.
                        state.copy(
                            loading = false,
                            loadingMore = false,
                            error = if (append) null else (error.message ?: "Could not load your library"),
                        )
                    },
                )
            }
        }
    }
}
