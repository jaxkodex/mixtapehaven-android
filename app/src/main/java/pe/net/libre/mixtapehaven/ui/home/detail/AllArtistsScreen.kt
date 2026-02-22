package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.EmptyScreen
import pe.net.libre.mixtapehaven.ui.components.ErrorScreen
import pe.net.libre.mixtapehaven.ui.components.ListScreenTopBar
import pe.net.libre.mixtapehaven.ui.components.LoadingScreen
import pe.net.libre.mixtapehaven.ui.components.PaginationLoadingRow
import pe.net.libre.mixtapehaven.ui.home.components.ArtistListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllArtistsScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onArtistMenuClick: (String) -> Unit = {}
) {
    val viewModel: AllArtistsViewModel = viewModel {
        AllArtistsViewModel(mediaRepository = mediaRepository)
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ListScreenTopBar(
                title = "All Artists",
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
                    onRetry = { viewModel.loadArtists() },
                    modifier = Modifier.fillMaxSize()
                )
                uiState.artists.isEmpty() -> EmptyScreen(
                    title = "No artists found",
                    subtitle = "Add media to your Jellyfin server",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val listState = rememberLazyListState()

                    LaunchedEffect(listState) {
                        snapshotFlow {
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        }.collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null) {
                                val totalItems = listState.layoutInfo.totalItemsCount
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
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                items(uiState.artists) { artist ->
                                    ArtistListItem(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) },
                                        onMenuClick = { onArtistMenuClick(artist.id) }
                                    )
                                }

                                if (uiState.isLoadingMore) {
                                    item { PaginationLoadingRow() }
                                }
                            }

                            AlphabetIndex(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlphabetIndex(
    modifier: Modifier = Modifier
) {
    val alphabet = listOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z", "#"
    )

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall,
                color = CyberNeonBlue,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable { /* TODO: Implement scroll to letter */ }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
