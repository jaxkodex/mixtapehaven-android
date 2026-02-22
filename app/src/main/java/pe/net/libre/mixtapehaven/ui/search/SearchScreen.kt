package pe.net.libre.mixtapehaven.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.api.SearchHint
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.data.util.NetworkUtil
import pe.net.libre.mixtapehaven.ui.components.FuturisticTextField
import pe.net.libre.mixtapehaven.ui.components.PlaylistActionHandler
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.home.Artist
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.home.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.home.components.ArtistCircle
import pe.net.libre.mixtapehaven.ui.home.components.PlaylistCard
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.WarningAmber

// Constants for grid calculations (in dp)
private const val ALBUM_CARD_HEIGHT_DP = 200
private const val MAX_GRID_HEIGHT_DP = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    mediaRepository: MediaRepository,
    offlineRepository: OfflineRepository,
    playbackManager: PlaybackManager,
    onNavigateBack: () -> Unit,
    onNavigateToArtistDetail: (String) -> Unit,
    onNavigateToPlaylistDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val networkProvider = remember { NetworkUtil.createProvider(context) }
    val viewModel: SearchViewModel = viewModel {
        SearchViewModel(
            mediaRepository = mediaRepository,
            offlineRepository = offlineRepository,
            playbackManager = playbackManager,
            networkProvider = networkProvider,
            onNavigateToArtistDetail = onNavigateToArtistDetail,
            onNavigateToPlaylistDetail = onNavigateToPlaylistDetail
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackManager.playbackState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FuturisticTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        label = "",
                        placeholder = "Search songs, artists, albums...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.onSearchSubmit()
                            }
                        ),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onClearSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = GunmetalGray
                                    )
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LunarWhite
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
        PlaylistActionHandler(
            mediaRepository = mediaRepository,
            playbackManager = playbackManager,
            enabled = !uiState.isOfflineMode
        ) { onSongMoreClick ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    // Show autocomplete hints
                    !uiState.hasSearched && uiState.hints.isNotEmpty() -> {
                        SearchHintsList(
                            hints = uiState.hints,
                            onHintClick = viewModel::onHintClick
                        )
                    }
                    // Show loading
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CyberNeonBlue,
                                strokeWidth = 4.dp
                            )
                        }
                    }
                    // Show results after search
                    uiState.hasSearched -> {
                        SearchResults(
                            uiState = uiState,
                            playbackState = playbackState,
                            onFilterChange = viewModel::onFilterChange,
                            onSongClick = viewModel::onSongClick,
                            onPlayPauseClick = { playbackManager.togglePlayPause() },
                            onAlbumClick = viewModel::onAlbumClick,
                            onArtistClick = viewModel::onArtistClick,
                            onPlaylistClick = viewModel::onPlaylistClick,
                            onSongMoreClick = onSongMoreClick
                        )
                    }
                    // Show initial state
                    else -> {
                        InitialSearchState()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHintsList(
    hints: List<SearchHint>,
    onHintClick: (SearchHint) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(hints) { hint ->
            SearchHintItem(
                hint = hint,
                onClick = { onHintClick(hint) }
            )
        }
    }
}

@Composable
private fun SearchHintItem(
    hint: SearchHint,
    onClick: () -> Unit
) {
    val (icon, typeLabel) = when (hint.type) {
        "Audio" -> Icons.Default.MusicNote to "Song"
        "MusicAlbum" -> Icons.Default.Album to "Album"
        "MusicArtist" -> Icons.Default.Person to "Artist"
        "Playlist" -> Icons.Default.PlaylistPlay to "Playlist"
        else -> Icons.Default.Search to "Item"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberNeonBlue,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = hint.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LunarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = when (hint.type) {
                "Audio" -> hint.artists?.firstOrNull() ?: hint.albumArtist
                "MusicAlbum" -> hint.albumArtist
                else -> null
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = GunmetalGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Type chip
        Box(
            modifier = Modifier
                .background(
                    color = GunmetalGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = CyberNeonBlue
            )
        }
    }

    HorizontalDivider(
        color = GunmetalGray.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    playbackState: pe.net.libre.mixtapehaven.data.playback.PlaybackState,
    onFilterChange: (SearchFilter) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Offline banner
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
                            text = "Offline Mode - Showing downloaded songs only",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LunarWhite
                        )
                    }
                }
            }
        }

        // Filter chips
        if (!uiState.isOfflineMode) {
            item {
                FilterChipsRow(
                    activeFilter = uiState.activeFilter,
                    onFilterChange = onFilterChange,
                    songsCount = uiState.songs.size,
                    albumsCount = uiState.albums.size,
                    artistsCount = uiState.artists.size,
                    playlistsCount = uiState.playlists.size
                )
            }
        }

        // Empty state
        if (uiState.songs.isEmpty() && uiState.albums.isEmpty() &&
            uiState.artists.isEmpty() && uiState.playlists.isEmpty()
        ) {
            item {
                EmptySearchResults()
            }
        } else {
            // Display results based on filter
            when (uiState.activeFilter) {
                SearchFilter.ALL -> {
                    // Mixed results
                    if (uiState.songs.isNotEmpty()) {
                        item {
                            SectionHeaderWithCount("Songs", uiState.songs.size)
                        }
                        itemsIndexed(uiState.songs.take(5)) { index, song ->
                            SongListItem(
                                song = song,
                                trackNumber = index + 1,
                                onClick = { onSongClick(song) },
                                isCurrentSong = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                onPlayPauseClick = onPlayPauseClick,
                                onMoreClick = { onSongMoreClick(song) }
                            )
                        }
                    }

                    if (uiState.albums.isNotEmpty()) {
                        item {
                            SectionHeaderWithCount("Albums", uiState.albums.size)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.albums) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.artists.isNotEmpty()) {
                        item {
                            SectionHeaderWithCount("Artists", uiState.artists.size)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.artists) { artist ->
                                    ArtistCircle(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.playlists.isNotEmpty()) {
                        item {
                            SectionHeaderWithCount("Playlists", uiState.playlists.size)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.playlists) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist) }
                                    )
                                }
                            }
                        }
                    }
                }

                SearchFilter.SONGS -> {
                    itemsIndexed(uiState.songs) { index, song ->
                        SongListItem(
                            song = song,
                            trackNumber = index + 1,
                            onClick = { onSongClick(song) },
                            isCurrentSong = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onPlayPauseClick = onPlayPauseClick,
                            onMoreClick = onSongMoreClick?.let { { it(song) } }
                        )
                    }
                }

                SearchFilter.ALBUMS -> {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height((uiState.albums.size * ALBUM_CARD_HEIGHT_DP).coerceAtMost(MAX_GRID_HEIGHT_DP).dp)
                        ) {
                            items(uiState.albums) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) }
                                )
                            }
                        }
                    }
                }

                SearchFilter.ARTISTS -> {
                    items(uiState.artists) { artist ->
                        ArtistListItemCompact(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }

                SearchFilter.PLAYLISTS -> {
                    items(uiState.playlists) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    songsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    playlistsCount: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChipItem(
                label = "All",
                count = songsCount + albumsCount + artistsCount + playlistsCount,
                isSelected = activeFilter == SearchFilter.ALL,
                onClick = { onFilterChange(SearchFilter.ALL) }
            )
        }
        item {
            FilterChipItem(
                label = "Songs",
                count = songsCount,
                isSelected = activeFilter == SearchFilter.SONGS,
                onClick = { onFilterChange(SearchFilter.SONGS) }
            )
        }
        item {
            FilterChipItem(
                label = "Albums",
                count = albumsCount,
                isSelected = activeFilter == SearchFilter.ALBUMS,
                onClick = { onFilterChange(SearchFilter.ALBUMS) }
            )
        }
        item {
            FilterChipItem(
                label = "Artists",
                count = artistsCount,
                isSelected = activeFilter == SearchFilter.ARTISTS,
                onClick = { onFilterChange(SearchFilter.ARTISTS) }
            )
        }
        item {
            FilterChipItem(
                label = "Playlists",
                count = playlistsCount,
                isSelected = activeFilter == SearchFilter.PLAYLISTS,
                onClick = { onFilterChange(SearchFilter.PLAYLISTS) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("$label ($count)") },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyberNeonBlue.copy(alpha = 0.2f),
            selectedLabelColor = CyberNeonBlue,
            containerColor = GunmetalGray.copy(alpha = 0.5f),
            labelColor = LunarWhite.copy(alpha = 0.7f)
        )
    )
}

@Composable
private fun SectionHeaderWithCount(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LunarWhite
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = CyberNeonBlue
        )
    }
}

@Composable
private fun ArtistListItemCompact(
    artist: Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artist image
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GunmetalGray),
            contentAlignment = Alignment.Center
        ) {
            if (artist.imageUrl != null) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = artist.name,
                    tint = LunarWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = LunarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.albumCount} Albums, ${artist.songCount} Songs",
                style = MaterialTheme.typography.bodySmall,
                color = GunmetalGray
            )
        }
    }

    HorizontalDivider(
        color = GunmetalGray.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PlaylistListItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist cover
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GunmetalGray),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl != null) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = playlist.name,
                    tint = LunarWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = LunarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = GunmetalGray
            )
        }
    }

    HorizontalDivider(
        color = GunmetalGray.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun EmptySearchResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = GunmetalGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.headlineSmall,
            color = LunarWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodyMedium,
            color = GunmetalGray
        )
    }
}

@Composable
private fun InitialSearchState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = GunmetalGray.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search your library",
            style = MaterialTheme.typography.headlineSmall,
            color = LunarWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Type to search for songs, artists, albums, and playlists",
            style = MaterialTheme.typography.bodyMedium,
            color = GunmetalGray,
            textAlign = TextAlign.Center
        )
    }
}
