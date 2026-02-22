package pe.net.libre.mixtapehaven.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkConnectivityProvider
import pe.net.libre.mixtapehaven.ui.components.ErrorScreen
import pe.net.libre.mixtapehaven.ui.components.LoadingScreen
import pe.net.libre.mixtapehaven.ui.components.MediaPlaybackActions
import pe.net.libre.mixtapehaven.ui.components.PlaylistActionHandler
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.VaporwaveMagenta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    offlineRepository: OfflineRepository,
    dataStoreManager: DataStoreManager,
    networkConnectivityProvider: NetworkConnectivityProvider,
    onNavigateBack: () -> Unit
) {
    val viewModel: PlaylistDetailViewModel = viewModel {
        PlaylistDetailViewModel(
            playlistId = playlistId,
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            offlineRepository = offlineRepository,
            dataStoreManager = dataStoreManager,
            networkConnectivityProvider = networkConnectivityProvider
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackManager.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LunarWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = LunarWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add to playlist */ },
                containerColor = CyberNeonBlue,
                contentColor = DeepSpaceBlack
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        PlaylistActionHandler(
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            enabled = true
        ) { onSongMoreClick ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> LoadingScreen(modifier = Modifier.fillMaxSize())
                    uiState.errorMessage != null -> ErrorScreen(
                        message = uiState.errorMessage ?: "An error occurred",
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                PlaylistCoverImage(
                                    coverUrl = uiState.playlist?.coverUrl,
                                    coverPlaceholder = uiState.playlist?.coverPlaceholder ?: "🎵"
                                )
                            }
                            item {
                                PlaylistMeta(
                                    name = uiState.playlist?.name ?: "",
                                    downloadedCount = uiState.downloadedCount,
                                    totalSongs = uiState.songs.size,
                                    totalDuration = uiState.totalDuration
                                )
                            }
                            item {
                                PlaylistActions(
                                    onPlayAll = { viewModel.onPlayAllClick() },
                                    onShuffle = { viewModel.onShuffleClick() },
                                    onInstantMix = { viewModel.onInstantMixClick() },
                                    isLoadingMix = uiState.isLoadingMix,
                                    onDownloadAll = { viewModel.onDownloadAllClick() },
                                    isDownloading = uiState.isDownloading
                                )
                            }
                            itemsIndexed(uiState.songs) { index, song ->
                                SongListItem(
                                    trackNumber = index + 1,
                                    song = song,
                                    onClick = { viewModel.onSongClick(song) },
                                    isCurrentSong = playbackState.currentSong?.id == song.id,
                                    isPlaying = playbackState.isPlaying,
                                    onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                    onMoreClick = { onSongMoreClick(song) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(160.dp))
                            }
                        }
                    }
                }

                if (uiState.showCellularConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onDismissCellularDialog() },
                        title = { Text("Download over mobile data?") },
                        text = {
                            Text(
                                "You are on mobile data. Downloading ${uiState.songs.size} songs may use significant data. Continue?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onConfirmCellularDownload() }) {
                                Text("Download")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onDismissCellularDialog() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCoverImage(
    coverUrl: String?,
    coverPlaceholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Playlist cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepSpaceBlack,
                                GunmetalGray.copy(alpha = 0.5f),
                                VaporwaveMagenta.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coverPlaceholder,
                    fontSize = 120.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistMeta(
    name: String,
    downloadedCount: Int,
    totalSongs: Int,
    totalDuration: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = LunarWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$totalSongs Tracks, $totalDuration",
                style = MaterialTheme.typography.bodyLarge,
                color = LunarWhite.copy(alpha = 0.6f)
            )
            if (downloadedCount > 0) {
                Icon(
                    imageVector = if (downloadedCount >= totalSongs) {
                        Icons.Default.CloudDone
                    } else {
                        Icons.Default.CloudQueue
                    },
                    contentDescription = if (downloadedCount >= totalSongs) {
                        "Fully downloaded"
                    } else {
                        "$downloadedCount of $totalSongs downloaded"
                    },
                    tint = CyberNeonBlue,
                    modifier = Modifier.size(18.dp)
                )
                if (downloadedCount < totalSongs) {
                    Text(
                        text = "$downloadedCount/$totalSongs",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberNeonBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistActions(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onInstantMix: () -> Unit,
    isLoadingMix: Boolean,
    onDownloadAll: () -> Unit,
    isDownloading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        MediaPlaybackActions(
            onPlayAll = onPlayAll,
            onShuffle = onShuffle,
            onInstantMix = onInstantMix,
            isLoadingMix = isLoadingMix
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDownloadAll,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GunmetalGray,
                contentColor = LunarWhite
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isDownloading
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    color = LunarWhite,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Download All",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
