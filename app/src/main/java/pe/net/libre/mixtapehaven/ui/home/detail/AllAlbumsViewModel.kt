package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Album

data class AllAlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
)

class AllAlbumsViewModel(
    private val mediaRepository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllAlbumsUiState())
    val uiState: StateFlow<AllAlbumsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                hasMorePages = true
            )

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { albums ->
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        isLoading = false,
                        errorMessage = null,
                        hasMorePages = albums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load albums"
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

            val currentSize = _uiState.value.albums.size

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = currentSize).fold(
                onSuccess = { newAlbums ->
                    _uiState.value = _uiState.value.copy(
                        albums = _uiState.value.albums + newAlbums,
                        isLoadingMore = false,
                        hasMorePages = newAlbums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Failed to load more albums"
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

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { albums ->
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        isRefreshing = false,
                        errorMessage = null,
                        hasMorePages = albums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to refresh albums"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
