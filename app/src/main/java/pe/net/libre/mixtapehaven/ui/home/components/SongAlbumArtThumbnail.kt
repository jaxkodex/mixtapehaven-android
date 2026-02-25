package pe.net.libre.mixtapehaven.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack

@Composable
internal fun SongAlbumArtThumbnail(
    albumCoverUrl: String?,
    albumCoverPlaceholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DeepSpaceBlack
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (albumCoverUrl != null) {
            AsyncImage(
                model = albumCoverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = albumCoverPlaceholder,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
