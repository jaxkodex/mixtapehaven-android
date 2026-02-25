package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.EmptyScreen
import pe.net.libre.mixtapehaven.ui.components.ErrorScreen
import pe.net.libre.mixtapehaven.ui.components.ListScreenTopBar
import pe.net.libre.mixtapehaven.ui.components.LoadingScreen
import pe.net.libre.mixtapehaven.ui.components.PaginatedRefreshableGrid
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAlbumsScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val viewModel: AllAlbumsViewModel = viewModel {
        AllAlbumsViewModel(mediaRepository = mediaRepository)
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ListScreenTopBar(
                title = "All Albums",
                onNavigateBack = onNavigateBack,
                onSearchClick = onSearchClick
            )
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingScreen(modifier = Modifier.fillMaxSize())
                uiState.errorMessage != null -> ErrorScreen(
                    message = uiState.errorMessage ?: "An error occurred",
                    onRetry = { viewModel.loadAlbums() },
                    modifier = Modifier.fillMaxSize()
                )
                uiState.albums.isEmpty() -> EmptyScreen(
                    title = "No albums found",
                    subtitle = "Add media to your Jellyfin server",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    PaginatedRefreshableGrid(
                        isRefreshing = uiState.isRefreshing,
                        isLoadingMore = uiState.isLoadingMore,
                        onRefresh = { viewModel.refresh() },
                        onLoadMore = { viewModel.loadMore() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.albums) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
