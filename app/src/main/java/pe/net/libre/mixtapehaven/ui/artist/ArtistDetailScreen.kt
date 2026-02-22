package pe.net.libre.mixtapehaven.ui.artist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.components.PlaylistActionHandler
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.home.Artist
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.VaporwaveMagenta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    onNavigateBack: () -> Unit
) {
    val viewModel: ArtistDetailViewModel = viewModel {
        ArtistDetailViewModel(
            artistId = artistId,
            mediaRepository = mediaRepository,
            playbackManager = playbackManager
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
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyberNeonBlue)
                        }
                    }
                    uiState.errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "An error occurred",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = LunarWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                ArtistHeroImage(artist = uiState.artist)
                            }
                            item {
                                ArtistMetadata(
                                    artist = uiState.artist,
                                    albumsSize = uiState.albums.size,
                                    songsSize = uiState.songs.size
                                )
                            }
                            item {
                                ArtistActionButtons(
                                    onPlayAll = { viewModel.onPlayAllClick() },
                                    onShuffle = { viewModel.onShuffleClick() },
                                    onInstantMix = { viewModel.onInstantMixClick() },
                                    isLoadingMix = uiState.isLoadingMix
                                )
                            }
                            item {
                                ArtistTabSelector(
                                    selectedTab = uiState.selectedTab,
                                    onTabChange = { viewModel.onTabChange(it) }
                                )
                            }

                            when (uiState.selectedTab) {
                                ArtistTab.ALBUMS -> {
                                    if (uiState.albums.isEmpty()) {
                                        item {
                                            ArtistEmptyState(message = "No albums found")
                                        }
                                    } else {
                                        item {
                                            ArtistAlbumsGrid(
                                                albums = uiState.albums,
                                                onAlbumClick = { viewModel.onAlbumClick(it) }
                                            )
                                        }
                                    }
                                }
                                ArtistTab.SONGS -> {
                                    if (uiState.songs.isEmpty()) {
                                        item {
                                            ArtistEmptyState(message = "No songs found")
                                        }
                                    } else {
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
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(160.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeroImage(
    artist: Artist?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist?.imageUrl != null) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = "Artist image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(VaporwaveMagenta, DeepSpaceBlack)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = LunarWhite.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistMetadata(
    artist: Artist?,
    albumsSize: Int,
    songsSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = artist?.name ?: "",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = LunarWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$albumsSize Albums • $songsSize Songs",
            style = MaterialTheme.typography.bodyLarge,
            color = LunarWhite.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ArtistActionButtons(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onInstantMix: () -> Unit,
    isLoadingMix: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberNeonBlue,
                    contentColor = DeepSpaceBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play All",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Button(
                onClick = onShuffle,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GunmetalGray,
                    contentColor = LunarWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shuffle",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onInstantMix,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaporwaveMagenta,
                contentColor = LunarWhite
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoadingMix
        ) {
            if (isLoadingMix) {
                CircularProgressIndicator(
                    color = LunarWhite,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Instant Mix",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun ArtistTabSelector(
    selectedTab: ArtistTab,
    onTabChange: (ArtistTab) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = CyberNeonBlue,
        modifier = modifier,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = CyberNeonBlue
                )
            }
        }
    ) {
        Tab(
            selected = selectedTab == ArtistTab.ALBUMS,
            onClick = { onTabChange(ArtistTab.ALBUMS) },
            text = {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        )
        Tab(
            selected = selectedTab == ArtistTab.SONGS,
            onClick = { onTabChange(ArtistTab.SONGS) },
            text = {
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        )
    }
}

@Composable
private fun ArtistAlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = (albums.size + 1) / 2
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.height((rows * 200).dp),
        userScrollEnabled = false
    ) {
        items(albums) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
private fun ArtistEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = LunarWhite.copy(alpha = 0.6f)
        )
    }
}
