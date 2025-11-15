package pe.net.libre.mixtapehaven.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Playlist cover
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
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
                Text(
                    text = playlist.coverPlaceholder,
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        // Playlist name
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            color = LunarWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Song count
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = GunmetalGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
