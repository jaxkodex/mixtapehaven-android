package pe.net.libre.mixtapehaven.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@Composable
fun SongListItem(
    song: Song,
    trackNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onPlayPauseClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number (highlight if current song)
        Text(
            text = trackNumber.toString().padStart(2, '0'),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCurrentSong) CyberNeonBlue else CyberNeonBlue.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 4.dp)
        )

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
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = song.albumCoverPlaceholder,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Song info (title and artist)
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Duration
        Text(
            text = song.duration,
            style = MaterialTheme.typography.bodySmall,
            color = GunmetalGray
        )

        // Download button
        if (onDownloadClick != null) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    song.downloadProgress != null -> {
                        // Show progress indicator when downloading
                        CircularProgressIndicator(
                            progress = { song.downloadProgress!! },
                            modifier = Modifier.size(24.dp),
                            color = CyberNeonBlue,
                            strokeWidth = 2.dp
                        )
                    }
                    song.isDownloaded -> {
                        // Show checkmark when downloaded
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = CyberNeonBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        // Show download button
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download",
                                tint = GunmetalGray
                            )
                        }
                    }
                }
            }
        }

        // Play/Pause button (only show for current song)
        if (onPlayPauseClick != null && isCurrentSong) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = CyberNeonBlue
                )
            }
        }
    }
}
