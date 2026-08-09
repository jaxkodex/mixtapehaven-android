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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.jellyfin.formatTimeLeft
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/** Fixed card width from the "Continue Card" component in happypath.pen. */
private val CARD_WIDTH = 184.dp

/** How much a card is faded when it cannot be played right now. */
private const val UNAVAILABLE_ALPHA = 0.45f

/**
 * A Continue watching card: a 16:9 still with a progress bar pinned to its bottom edge, the title,
 * and a meta line ("S2 E4 · 23 min left"). [downloaded] shows the offline check beside the meta.
 *
 * [unavailable] marks a title that cannot start right now — offline with no saved copy. It is
 * dimmed and says so instead of the time left, because a card that looks identical to a playable
 * one turns a tap into an apparent no-op. It stays clickable so the tap can explain itself.
 *
 * Mirrors the "Continue Card" component in happypath.pen.
 */
@Composable
fun ContinueCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloaded: Boolean = false,
    unavailable: Boolean = false,
) {
    val meta = if (unavailable) {
        listOfNotNull(video.seasonEpisodeLabel, "Not downloaded").joinToString(" · ")
    } else {
        listOfNotNull(
            video.seasonEpisodeLabel,
            formatTimeLeft(video.runtimeMs, video.resumePositionMs).ifEmpty { null },
        ).joinToString(" · ")
    }
    val clickLabel = if (unavailable) "${video.title}, not available offline" else "Resume ${video.title}"
    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContinueStill(video, dimmed = unavailable)
        Text(
            video.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (unavailable) TextPrimary.copy(alpha = UNAVAILABLE_ALPHA) else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            ContinueMeta(meta = meta, downloaded = downloaded, unavailable = unavailable)
        }
    }
}

/** The 16:9 still with the watched-progress bar across its bottom edge. */
@Composable
private fun ContinueStill(video: VideoItem, dimmed: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .alpha(if (dimmed) UNAVAILABLE_ALPHA else 1f)
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

/**
 * The meta line under the title, prefixed with the offline-copy check or — when the title cannot
 * play right now — the cloud-off marker that names the reason.
 */
@Composable
private fun ContinueMeta(meta: String, downloaded: Boolean, unavailable: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when {
            unavailable -> Icon(
                Icons.Outlined.CloudOff,
                contentDescription = "Not available offline",
                tint = TextMuted,
                modifier = Modifier.size(14.dp),
            )
            downloaded -> Icon(
                Icons.Outlined.DownloadDone,
                contentDescription = "Downloaded",
                tint = Accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            meta,
            style = MaterialTheme.typography.bodySmall,
            color = if (unavailable) TextMuted else TextSecondary,
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
