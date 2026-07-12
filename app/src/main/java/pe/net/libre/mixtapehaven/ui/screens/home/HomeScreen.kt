package pe.net.libre.mixtapehaven.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.ui.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.components.RandomWalkCard
import pe.net.libre.mixtapehaven.ui.components.SearchField
import pe.net.libre.mixtapehaven.ui.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.components.StatusPill
import pe.net.libre.mixtapehaven.ui.components.SurfaceCard
import pe.net.libre.mixtapehaven.ui.components.TrackRow
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.Stroke
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel {
        HomeViewModel(it.repository, it.playerController, it.downloadManager, it.diagnosticsLog)
    }
    val state by viewModel.state.collectAsState()
    val hasDownloads = state.onDevice.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.size(0.dp))

            HomeHeader(
                userName = state.userName,
                hasDownloads = hasDownloads,
                onOpenSettings = onOpenSettings,
            )

            RandomWalkCard(
                onPlay = {
                    viewModel.startRandomWalk()
                    onOpenNowPlaying()
                },
            )

            SearchField(
                placeholder = "Search songs, albums, artists",
                onClick = onOpenSearch,
            )

            if (hasDownloads) {
                OnDeviceSection(
                    tracks = state.onDevice,
                    onManage = onOpenDownloads,
                    onPlay = { track ->
                        viewModel.playOnDevice(track)
                        onOpenNowPlaying()
                    },
                )
            }

            SectionHeader(
                title = "Recently added",
                actionLabel = "See all",
                onAction = onOpenDownloads,
            )

            when {
                state.albums.isNotEmpty() -> AlbumGrid(
                    albums = state.albums,
                    onAlbumClick = { album ->
                        viewModel.playAlbum(album)
                        onOpenNowPlaying()
                    },
                )
                // Fetch in flight: render nothing rather than flash an empty state.
                state.loading -> Unit
                state.error != null -> OfflineAlbumsState(onRetry = viewModel::load)
                else -> FirstRunEmptyState(onOpenSettings = onOpenSettings)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

/** Max number of saved tracks previewed in the Home "On your device" section. */
private const val ON_DEVICE_PREVIEW = 4

@Composable
private fun HomeHeader(
    userName: String,
    hasDownloads: Boolean,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                "Good evening",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                userName.ifBlank { "Mixtape" },
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusPill(
                text = if (hasDownloads) "Offline ready" else "Online",
                dotColor = if (hasDownloads) Accent else Color(0xFF7BB661),
            )
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onOpenSettings),
            )
        }
    }
}

@Composable
private fun OnDeviceSection(
    tracks: List<Track>,
    onManage: () -> Unit,
    onPlay: (Track) -> Unit,
) {
    SectionHeader(
        title = "On your device",
        actionLabel = "Manage",
        onAction = onManage,
    )
    Column {
        tracks.take(ON_DEVICE_PREVIEW).forEach { track ->
            TrackRow(track = track, onClick = { onPlay(track) })
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        albums.chunked(2).forEach { rowAlbums ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowAlbums.forEach { album ->
                    AlbumCard(
                        album = album,
                        modifier = Modifier.weight(1f),
                        onClick = { onAlbumClick(album) },
                    )
                }
                if (rowAlbums.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Shown under "Recently added" when the album fetch failed (e.g. offline): the library isn't
 * empty, we just can't reach the server, so the copy must not claim "nothing's here yet". */
@Composable
private fun OfflineAlbumsState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                "You're offline",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                "Recently added albums will show up when you're back online.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.dp, Stroke, CircleShape)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Try again",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun FirstRunEmptyState(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                "Saves as you listen",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                "Nothing's here yet. Play anything and it's kept for offline automatically. Turn this off anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.dp, Stroke, CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Auto-download settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                )
            }
        }
    }
}
