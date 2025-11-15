package pe.net.libre.mixtapehaven.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.ArtistCircle
import pe.net.libre.mixtapehaven.ui.home.components.NowPlayingBar
import pe.net.libre.mixtapehaven.ui.home.components.PlaylistCard
import pe.net.libre.mixtapehaven.ui.home.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.VaporwaveMagenta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    onNavigateToAllAlbums: () -> Unit = {},
    onNavigateToAllArtists: () -> Unit = {},
    onNavigateToAllSongs: () -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            onNavigateToAllAlbums = onNavigateToAllAlbums,
            onNavigateToAllArtists = onNavigateToAllArtists,
            onNavigateToAllSongs = onNavigateToAllSongs,
            onNavigateToNowPlaying = onNavigateToNowPlaying,
            onLogout = onLogout
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackManager.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LunarWhite,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.onProfileClick() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(VaporwaveMagenta, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = LunarWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onSearchClick() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyberNeonBlue,
                            modifier = Modifier.size(28.dp)
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
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Recently Added Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            title = "Recently Added",
                            onSeeMoreClick = { viewModel.onSeeMoreClick("recently_added") }
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

                    // Top Artists Section
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

                    // Your Playlist Section
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = "Your Playlist",
                            onSeeMoreClick = { viewModel.onSeeMoreClick("playlists") }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.playlists) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { viewModel.onPlaylistClick(playlist) }
                                )
                            }
                        }
                    }

                    // Popular Songs Section
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
                            onClick = { viewModel.onSongClick(song) }
                        )
                    }
                }
            }

            // Floating Now Playing Bar
            NowPlayingBar(
                playbackState = playbackState,
                onPlayPauseClick = { viewModel.onPlayPauseClick() },
                onBarClick = { viewModel.onNowPlayingBarClick() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
