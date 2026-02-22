package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    isLoading: Boolean,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Title
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                color = LunarWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // New playlist row
            ListItem(
                headlineContent = {
                    Text(
                        text = "New playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberNeonBlue
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New playlist",
                        tint = CyberNeonBlue
                    )
                },
                modifier = Modifier.clickable(onClick = onCreateNew)
            )

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = GunmetalGray.copy(alpha = 0.5f)
            )

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberNeonBlue)
                }
            } else {
                // Existing playlists list
                LazyColumn {
                    items(playlists) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onSelectPlaylist(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LunarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = GunmetalGray
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GunmetalGray),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverUrl != null) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = playlist.coverPlaceholder,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
