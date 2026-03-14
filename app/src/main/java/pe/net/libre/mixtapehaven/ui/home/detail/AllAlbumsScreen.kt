package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.EmptyScreen
import pe.net.libre.mixtapehaven.ui.components.ErrorScreen
import pe.net.libre.mixtapehaven.ui.components.LoadingScreen
import pe.net.libre.mixtapehaven.ui.components.PaginatedRefreshableGrid
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAlbumsScreen(
    mediaRepository: MediaRepository,
    onAlbumClick: (String) -> Unit = {},
) {
    val viewModel: AllAlbumsViewModel = viewModel {
        AllAlbumsViewModel(mediaRepository = mediaRepository)
    }
    val uiState by viewModel.uiState.collectAsState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 0.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All albums",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(
                onClick = {},
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = TextPrimary
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
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
