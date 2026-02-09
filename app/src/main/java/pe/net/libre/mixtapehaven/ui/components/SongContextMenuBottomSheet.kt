package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
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
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenuBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
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
            // Song header row (non-interactive, for context)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GunmetalGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumCoverUrl != null) {
                        AsyncImage(
                            model = song.albumCoverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = song.albumCoverPlaceholder,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Song info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunarWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = GunmetalGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = GunmetalGray.copy(alpha = 0.5f)
            )

            // Add to playlist action
            ListItem(
                headlineContent = {
                    Text(
                        text = "Add to playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunarWhite
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = GunmetalGray
                    )
                },
                modifier = Modifier.clickable {
                    onAddToPlaylist(song)
                }
            )
        }
    }
}
