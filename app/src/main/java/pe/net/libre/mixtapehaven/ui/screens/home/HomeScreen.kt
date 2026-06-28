package pe.net.libre.mixtapehaven.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.SampleData
import pe.net.libre.mixtapehaven.ui.components.AlbumCard
import pe.net.libre.mixtapehaven.ui.components.RandomWalkCard
import pe.net.libre.mixtapehaven.ui.components.SearchField
import pe.net.libre.mixtapehaven.ui.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.components.StatusPill
import pe.net.libre.mixtapehaven.ui.components.SurfaceCard
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.size(0.dp))

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    SampleData.GREETING,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    SampleData.USER_NAME,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (SampleData.FIRST_RUN) {
                    StatusPill(text = "Online only", dotColor = TextMuted)
                } else {
                    StatusPill(text = "Offline ready", dotColor = Color(0xFF7BB661))
                }
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

        RandomWalkCard(onPlay = onOpenNowPlaying)

        SearchField(
            placeholder = "Search songs, albums, artists",
            onClick = onOpenSearch,
        )

        SectionHeader(
            title = "On your device",
            actionLabel = "See all",
            onAction = onOpenDownloads,
        )

        if (SampleData.FIRST_RUN) {
            FirstRunEmptyState(onOpenSettings = onOpenSettings)
        } else {
            AlbumGrid(albums = SampleData.onDevice, onAlbumClick = onOpenNowPlaying)
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: () -> Unit,
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
                        onClick = onAlbumClick,
                    )
                }
                if (rowAlbums.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
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
