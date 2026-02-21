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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
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
    onMoreClick: (() -> Unit)? = null,
    showCardStyle: Boolean = false
) {
    if (showCardStyle) {
        CardStyleSongItem(
            song = song,
            onClick = onClick,
            modifier = modifier,
            isCurrentSong = isCurrentSong,
            isPlaying = isPlaying,
            onPlayPauseClick = onPlayPauseClick
        )
    } else {
        ClassicSongItem(
            song = song,
            trackNumber = trackNumber,
            onClick = onClick,
            modifier = modifier,
            isCurrentSong = isCurrentSong,
            isPlaying = isPlaying,
            onPlayPauseClick = onPlayPauseClick,
            onMoreClick = onMoreClick
        )
    }
}

@Composable
private fun CardStyleSongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onPlayPauseClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = GunmetalGray,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepSpaceBlack),
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
                    text = buildString {
                        append(song.artist)
                        if (song.albumName.isNotEmpty()) {
                            append(" \u2022 ")
                            append(song.albumName)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LunarWhite.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Play/Pause button (always visible)
            IconButton(
                onClick = {
                    if (isCurrentSong && onPlayPauseClick != null) {
                        onPlayPauseClick()
                    } else {
                        onClick()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(CyberNeonBlue, CircleShape)
            ) {
                Icon(
                    imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentSong && isPlaying) "Pause" else "Play",
                    tint = DeepSpaceBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ClassicSongItem(
    song: Song,
    trackNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onPlayPauseClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunarWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Downloaded",
                        tint = CyberNeonBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Duration
        Text(
            text = song.duration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // More button (three dots)
        if (onMoreClick != null) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
