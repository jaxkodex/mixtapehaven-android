package pe.net.libre.mixtapehaven.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.PlaylistActionHandler
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.ArtistCircle
import pe.net.libre.mixtapehaven.ui.home.components.PlaylistCard
import pe.net.libre.mixtapehaven.ui.home.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.VaporwaveMagenta
import pe.net.libre.mixtapehaven.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    offlineRepository: pe.net.libre.mixtapehaven.data.repository.OfflineRepository,
    dataStoreManager: pe.net.libre.mixtapehaven.data.preferences.DataStoreManager,
    onNavigateToAllAlbums: () -> Unit = {},
    onNavigateToAllArtists: () -> Unit = {},
    onNavigateToAllSongs: () -> Unit = {},
    onNavigateToAllPlaylists: () -> Unit = {},
    onNavigateToPlaylistDetail: (String) -> Unit = {},
    onNavigateToArtistDetail: (String) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            offlineRepository = offlineRepository,
            dataStoreManager = dataStoreManager,
            context = context,
            onNavigateToAllAlbums = onNavigateToAllAlbums,
            onNavigateToAllArtists = onNavigateToAllArtists,
            onNavigateToAllSongs = onNavigateToAllSongs,
            onNavigateToAllPlaylists = onNavigateToAllPlaylists,
            onNavigateToPlaylistDetail = onNavigateToPlaylistDetail,
            onNavigateToArtistDetail = onNavigateToArtistDetail,
            onNavigateToNowPlaying = onNavigateToNowPlaying,
            onNavigateToSearch = onNavigateToSearch
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackManager.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {

                Column {
                    Text(
                        text = if (uiState.isOfflineMode) "Mixtape Haven (Offline)" else "Mixtape Haven",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaporwaveMagenta
                    )
                    if (uiState.serverName.isNotEmpty()) {
                        Text(
                            text = "JELLYFIN SERVER: ${uiState.serverName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LunarWhite.copy(alpha = 0.4f)
                        )
                    }
                }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE87C5E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = LunarWhite
                            )
                        }
                        if (!uiState.isOfflineMode) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, DeepSpaceBlack, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack
                )
            )
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        PlaylistActionHandler(
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            enabled = !uiState.isOfflineMode,
            onPlaylistChanged = { viewModel.refreshPlaylists() },
            onDownloadSong = { song -> viewModel.onDownloadClick(song) }
        ) { onSongMoreClick ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = CyberNeonBlue,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading your library...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LunarWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Content state
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Offline Mode Banner
                    if (uiState.isOfflineMode) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = WarningAmber.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = WarningAmber
                                    )
                                    Text(
                                        text = "Offline Mode - Showing downloaded content",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LunarWhite
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isOfflineMode) {
                        // Offline mode: show only downloaded songs
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(
                                title = "Downloaded Songs",
                                onSeeMoreClick = null
                            )
                        }

                        if (uiState.downloadedSongs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = LunarWhite.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No offline content available",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = LunarWhite
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Download songs while online to access them offline",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LunarWhite.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.retry() }) {
                                        Text("Retry Connection")
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(uiState.downloadedSongs) { index, song ->
                                SongListItem(
                                    song = song,
                                    trackNumber = index + 1,
                                    onClick = { viewModel.onSongClick(song) },
                                    isCurrentSong = playbackState.currentSong?.id == song.id,
                                    isPlaying = playbackState.isPlaying,
                                    onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                    onMoreClick = null
                                )
                            }
                        }
                    } else {
                        // Normal online mode: show all sections

                        // Recently Added Section
                        if (uiState.recentlyAddedAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                SectionHeader(
                                    title = "Recently Added",
                                    onSeeMoreClick = { viewModel.onSeeMoreClick("recently_added") },
                                    actionText = "View All"
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.recentlyAddedAlbums) { album ->
                                        AlbumCard(
                                            album = album,
                                            onClick = { viewModel.onAlbumClick(album) }
                                        )
                                    }
                                }
                            }
                        }

                        // Top Artists Section
                        if (uiState.topArtists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(
                                    title = "Top Artists",
                                    onSeeMoreClick = { viewModel.onSeeMoreClick("top_artists") }
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.topArtists) { artist ->
                                        ArtistCircle(
                                            artist = artist,
                                            onClick = { viewModel.onArtistClick(artist) }
                                        )
                                    }
                                }
                            }
                        }

                        // Your Mixes Section (2-column grid)
                        if (uiState.playlists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(
                                    title = "Your Mixes",
                                    onSeeMoreClick = { viewModel.onSeeMoreClick("playlists") }
                                )
                            }

                            item {
                                val chunkedPlaylists = uiState.playlists.chunked(2)
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    chunkedPlaylists.forEach { rowPlaylists ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowPlaylists.forEach { playlist ->
                                                PlaylistCard(
                                                    playlist = playlist,
                                                    onClick = { viewModel.onPlaylistClick(playlist) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowPlaylists.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Popular Songs Section
                        if (uiState.popularSongs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(
                                    title = "Popular Songs",
                                    onSeeMoreClick = { viewModel.onSeeMoreClick("popular_songs") }
                                )
                            }

                            itemsIndexed(uiState.popularSongs) { index, song ->
                                SongListItem(
                                    song = song,
                                    trackNumber = index + 1,
                                    onClick = { viewModel.onSongClick(song) },
                                    isCurrentSong = playbackState.currentSong?.id == song.id,
                                    isPlaying = playbackState.isPlaying,
                                    onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                    onMoreClick = { onSongMoreClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
