package pe.net.libre.mixtapehaven.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import pe.net.libre.mixtapehaven.ui.theme.MixtapeHavenTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.OfflineBanner
import pe.net.libre.mixtapehaven.ui.components.PlaylistActionHandler
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.ArtistCircle
import pe.net.libre.mixtapehaven.ui.home.components.PlaylistCard
import pe.net.libre.mixtapehaven.ui.home.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.Border
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary
import java.util.Calendar

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

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
        containerColor = MaterialTheme.colorScheme.background
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
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading your library...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
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
                            item {
                                HomeGreetingHeader()
                            }
                            item {
                                HomeSearchBar(onNavigateToSearch = onNavigateToSearch)
                            }
                            if (uiState.recentlyAddedAlbums.isNotEmpty()) {
                                item {
                                    HomeRecentlyAddedSection(
                                        albums = uiState.recentlyAddedAlbums,
                                        onSeeMoreClick = { viewModel.onSeeMoreClick("recently_added") },
                                        onAlbumClick = { viewModel.onAlbumClick(it) }
                                    )
                                }
                            }
                            if (uiState.popularSongs.isNotEmpty()) {
                                item {
                                    HomePopularSongsSection(
                                        songs = uiState.popularSongs,
                                        currentSongId = playbackState.currentSong?.id,
                                        isPlaying = playbackState.isPlaying,
                                        onSeeMoreClick = null,
                                        onSongClick = { viewModel.onSongClick(it) },
                                        onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                        onSongMoreClick = { onSongMoreClick(it) }
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGreetingHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val greeting = remember { greetingText() }
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        IconButton(
            onClick = { /* TODO: Navigate to notifications screen */ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun HomeSearchBar(
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp)
            .height(48.dp)
            .clickable { onNavigateToSearch() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Search songs, artists...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No offline content available",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download songs while online to access them offline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(
            title = "FEATURED",
            onSeeMoreClick = onSeeMoreClick,
            actionText = "See All",
            accentBar = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
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
}

@Composable
private fun HomePopularSongsSection(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSeeMoreClick: (() -> Unit)?,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSongMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(
            title = "RECENTLY PLAYED",
            onSeeMoreClick = onSeeMoreClick,
            accentBar = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            songs.forEachIndexed { index, song ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = Border)
                }
                SongListItem(
                    song = song,
                    trackNumber = index + 1,
                    onClick = { onSongClick(song) },
                    isCurrentSong = currentSongId == song.id,
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    onMoreClick = { onSongMoreClick(song) },
                    showTrackNumber = false
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Home – Greeting Header")
@Composable
private fun PreviewHomeGreetingHeader() {
    MixtapeHavenTheme {
        HomeGreetingHeader()
    }
}

@Preview(showBackground = true, name = "Home – Search Bar")
@Composable
private fun PreviewHomeSearchBar() {
    MixtapeHavenTheme {
        HomeSearchBar(onNavigateToSearch = {})
    }
}

@Preview(showBackground = true, name = "Home – Recently Played Section")
@Composable
private fun PreviewHomePopularSongsSection() {
    MixtapeHavenTheme {
        HomePopularSongsSection(
            songs = mockPopularSongs,
            currentSongId = mockPopularSongs.first().id,
            isPlaying = true,
            onSeeMoreClick = {},
            onSongClick = {},
            onPlayPauseClick = {},
            onSongMoreClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Home – Full Content")
@Composable
private fun PreviewHomeFullContent() {
    MixtapeHavenTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            HomeGreetingHeader()
            HomeSearchBar(onNavigateToSearch = {})
            HomeRecentlyAddedSection(
                albums = mockRecentlyAddedAlbums,
                onSeeMoreClick = {},
                onAlbumClick = {}
            )
            HomePopularSongsSection(
                songs = mockPopularSongs,
                currentSongId = mockPopularSongs[1].id,
                isPlaying = false,
                onSeeMoreClick = {},
                onSongClick = {},
                onPlayPauseClick = {},
                onSongMoreClick = {}
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
