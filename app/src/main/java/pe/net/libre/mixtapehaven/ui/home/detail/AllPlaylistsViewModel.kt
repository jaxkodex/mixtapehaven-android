package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Playlist

data class AllPlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
)

class AllPlaylistsViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllPlaylistsUiState())
    val uiState: StateFlow<AllPlaylistsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                hasMorePages = true
            )

            mediaRepository.getUserPlaylists(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { playlists ->
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        isLoading = false,
                        errorMessage = null,
                        hasMorePages = playlists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load playlists"
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

            val currentSize = _uiState.value.playlists.size

            mediaRepository.getUserPlaylists(limit = PAGE_SIZE, startIndex = currentSize).fold(
                onSuccess = { newPlaylists ->
                    _uiState.value = _uiState.value.copy(
                        playlists = _uiState.value.playlists + newPlaylists,
                        isLoadingMore = false,
                        hasMorePages = newPlaylists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Failed to load more playlists"
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

            mediaRepository.getUserPlaylists(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { playlists ->
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        isRefreshing = false,
                        errorMessage = null,
                        hasMorePages = playlists.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to refresh playlists"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
