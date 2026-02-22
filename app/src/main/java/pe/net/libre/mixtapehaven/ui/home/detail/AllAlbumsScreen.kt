package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.EmptyScreen
import pe.net.libre.mixtapehaven.ui.components.ErrorScreen
import pe.net.libre.mixtapehaven.ui.components.ListScreenTopBar
import pe.net.libre.mixtapehaven.ui.components.LoadingScreen
import pe.net.libre.mixtapehaven.ui.components.PaginationLoadingRow
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
                    val gridState = rememberLazyGridState()

                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        }.collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null) {
                                val totalItems = gridState.layoutInfo.totalItemsCount
                                if (lastVisibleIndex >= totalItems - 5 && !uiState.isLoadingMore) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.albums) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) }
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item(span = { GridItemSpan(2) }) {
                                    PaginationLoadingRow()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
