package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.NowPlayingBar
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAlbumsScreen(
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    onNavigateBack: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {}
) {
    val viewModel: AllAlbumsViewModel = viewModel {
        AllAlbumsViewModel(mediaRepository = mediaRepository)
    }
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackManager.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Albums",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LunarWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberNeonBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyberNeonBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack
                )
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
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CyberNeonBlue
                    )
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "An error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LunarWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAlbums() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.albums.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No albums found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = LunarWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add media to your Jellyfin server",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LunarWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = CyberNeonBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Now Playing Bar
            NowPlayingBar(
                playbackState = playbackState,
                onPlayPauseClick = { playbackManager.togglePlayPause() },
                onBarClick = onNavigateToNowPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
