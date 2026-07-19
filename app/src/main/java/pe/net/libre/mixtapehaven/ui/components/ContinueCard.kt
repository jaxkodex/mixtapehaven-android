package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.jellyfin.formatTimeLeft
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/** Fixed card width from the "Continue Card" component in happypath.pen. */
private val CARD_WIDTH = 184.dp

/**
 * A Continue watching card: a 16:9 still with a progress bar pinned to its bottom edge, the title,
 * and a meta line ("S2 E4 · 23 min left"). [downloaded] shows the offline check beside the meta.
 *
 * Mirrors the "Continue Card" component in happypath.pen.
 */
@Composable
fun ContinueCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloaded: Boolean = false,
) {
    val meta = listOfNotNull(
        video.seasonEpisodeLabel,
        formatTimeLeft(video.runtimeMs, video.resumePositionMs).ifEmpty { null },
    ).joinToString(" · ")
    Column(
        modifier = modifier.width(CARD_WIDTH).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContinueStill(video)
        Text(
            video.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            ContinueMeta(meta = meta, downloaded = downloaded)
        }
    }
}

/** The 16:9 still with the watched-progress bar across its bottom edge. */
@Composable
private fun ContinueStill(video: VideoItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(video.artColor),
    ) {
        // Episodes store a 16:9 still as their PRIMARY image, so the poster is the better crop for
        // them; movies and series only have a wide image under backdropUrl.
        val stillUrl = if (video.kind == VideoKind.EPISODE) {
            video.posterUrl ?: video.backdropUrl
        } else {
            video.backdropUrl ?: video.posterUrl
        }
        if (stillUrl != null) {
            AsyncImage(
                model = stillUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        ProgressTrack(
            fraction = video.progressFraction,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The meta line under the title, optionally prefixed with the offline-copy check. */
@Composable
private fun ContinueMeta(meta: String, downloaded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (downloaded) {
            Icon(
                Icons.Outlined.DownloadDone,
                contentDescription = "Downloaded",
                tint = Accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            meta,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The 3.dp watched-progress bar drawn across the bottom of a Continue watching still. */
@Composable
private fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.25f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(Accent),
        )
    }
}
