package pe.net.libre.mixtapehaven.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Album cover (placeholder for now)
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(GunmetalGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = album.coverPlaceholder,
                style = MaterialTheme.typography.displayLarge
            )
        }

        // Album title
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            color = LunarWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Artist name
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = GunmetalGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
