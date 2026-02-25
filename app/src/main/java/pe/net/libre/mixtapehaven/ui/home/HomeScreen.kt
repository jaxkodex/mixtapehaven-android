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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
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
import pe.net.libre.mixtapehaven.ui.components.OfflineBanner

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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (uiState.isOfflineMode) {
                            item {
                                HomeOfflineSongsList(
                                    songs = uiState.downloadedSongs,
                                    currentSongId = playbackState.currentSong?.id,
                                    isPlaying = playbackState.isPlaying,
                                    onRetry = { viewModel.retry() },
                                    onSongClick = { viewModel.onSongClick(it) },
                                    onPlayPauseClick = { viewModel.onPlayPauseClick() }
                                )
                            }
                        } else {
                            if (uiState.recentlyAddedAlbums.isNotEmpty()) {
                                item {
                                    HomeRecentlyAddedSection(
                                        albums = uiState.recentlyAddedAlbums,
                                        onSeeMoreClick = { viewModel.onSeeMoreClick("recently_added") },
                                        onAlbumClick = { viewModel.onAlbumClick(it) }
                                    )
                                }
                            }
                            if (uiState.topArtists.isNotEmpty()) {
                                item {
                                    HomeTopArtistsSection(
                                        artists = uiState.topArtists,
                                        onSeeMoreClick = { viewModel.onSeeMoreClick("top_artists") },
                                        onArtistClick = { viewModel.onArtistClick(it) }
                                    )
                                }
                            }
                            if (uiState.playlists.isNotEmpty()) {
                                item {
                                    HomePlaylistsSection(
                                        playlists = uiState.playlists,
                                        onSeeMoreClick = { viewModel.onSeeMoreClick("playlists") },
                                        onPlaylistClick = { viewModel.onPlaylistClick(it) }
                                    )
                                }
                            }
                            if (uiState.popularSongs.isNotEmpty()) {
                                item {
                                    HomePopularSongsSection(
                                        songs = uiState.popularSongs,
                                        currentSongId = playbackState.currentSong?.id,
                                        isPlaying = playbackState.isPlaying,
                                        onSeeMoreClick = { viewModel.onSeeMoreClick("popular_songs") },
                                        onSongClick = { viewModel.onSongClick(it) },
                                        onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                        onSongMoreClick = { onSongMoreClick(it) }
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

@Composable
private fun HomeOfflineEmptyContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
        Button(onClick = onRetry) {
            Text("Retry Connection")
        }
    }
}

@Composable
private fun HomeRecentlyAddedSection(
    albums: List<Album>,
    onSeeMoreClick: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(
            title = "Recently Added",
            onSeeMoreClick = onSeeMoreClick,
            actionText = "View All"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albums) { album ->
                AlbumCard(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
private fun HomeTopArtistsSection(
    artists: List<Artist>,
    onSeeMoreClick: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(
            title = "Top Artists",
            onSeeMoreClick = onSeeMoreClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artists) { artist ->
                ArtistCircle(artist = artist, onClick = { onArtistClick(artist) })
            }
        }
    }
}

@Composable
private fun HomeOfflineSongsList(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onRetry: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OfflineBanner()
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(title = "Downloaded Songs", onSeeMoreClick = null)
        if (songs.isEmpty()) {
            HomeOfflineEmptyContent(onRetry = onRetry)
        } else {
            songs.forEachIndexed { index, song ->
                SongListItem(
                    song = song,
                    trackNumber = index + 1,
                    onClick = { onSongClick(song) },
                    isCurrentSong = currentSongId == song.id,
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    onMoreClick = null
                )
            }
        }
    }
}

@Composable
private fun HomePopularSongsSection(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSeeMoreClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSongMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(title = "Popular Songs", onSeeMoreClick = onSeeMoreClick)
        songs.forEachIndexed { index, song ->
            SongListItem(
                song = song,
                trackNumber = index + 1,
                onClick = { onSongClick(song) },
                isCurrentSong = currentSongId == song.id,
                isPlaying = isPlaying,
                onPlayPauseClick = onPlayPauseClick,
                onMoreClick = { onSongMoreClick(song) }
            )
        }
    }
}

@Composable
private fun HomePlaylistsSection(
    playlists: List<Playlist>,
    onSeeMoreClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(
            title = "Your Mixes",
            onSeeMoreClick = onSeeMoreClick
        )
        val chunkedPlaylists = playlists.chunked(2)
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
                            onClick = { onPlaylistClick(playlist) },
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
