package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Artist

data class AllArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val groupedArtists: Map<Char, List<Artist>> = emptyMap(),
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
)

class AllArtistsViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllArtistsUiState())
    val uiState: StateFlow<AllArtistsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        loadArtists()
    }

    fun loadArtists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                hasMorePages = true
            )

            mediaRepository.getAllArtists(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { artists ->
                    val groupedArtists = artists
                        .sortedBy { it.name.uppercase() }
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    _uiState.value = _uiState.value.copy(
                        artists = artists.sortedBy { it.name.uppercase() },
                        groupedArtists = groupedArtists,
                        isLoading = false,
                        errorMessage = null,
                        hasMorePages = artists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load artists"
                    )
                }
            )
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val currentSize = _uiState.value.artists.size

            mediaRepository.getAllArtists(limit = PAGE_SIZE, startIndex = currentSize).fold(
                onSuccess = { newArtists ->
                    val allArtists = (_uiState.value.artists + newArtists)
                        .sortedBy { it.name.uppercase() }

                    val groupedArtists = allArtists
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    _uiState.value = _uiState.value.copy(
                        artists = allArtists,
                        groupedArtists = groupedArtists,
                        isLoadingMore = false,
                        hasMorePages = newArtists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Failed to load more artists"
                    )
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                errorMessage = null,
                hasMorePages = true
            )

            mediaRepository.getAllArtists(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { artists ->
                    val groupedArtists = artists
                        .sortedBy { it.name.uppercase() }
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    _uiState.value = _uiState.value.copy(
                        artists = artists.sortedBy { it.name.uppercase() },
                        groupedArtists = groupedArtists,
                        isRefreshing = false,
                        errorMessage = null,
                        hasMorePages = artists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to refresh artists"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
