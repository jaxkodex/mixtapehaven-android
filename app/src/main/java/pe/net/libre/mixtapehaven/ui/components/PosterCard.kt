package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/** Width of a poster in a horizontal rail. The library grid sizes its own from the cell instead. */
val POSTER_RAIL_WIDTH = 120.dp

/**
 * Portrait 2:3 poster tile for a movie or TV series.
 *
 * The caller sizes it — a rail pins [POSTER_RAIL_WIDTH], the library grid lets the cell decide —
 * so no width is baked in here.
 */
@Composable
fun PosterCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // An episode is placed by its series and number, not its year: search can surface an episode
    // and its own series side by side, and "2006 · 53m" does not tell them apart.
    val meta = when (video.kind) {
        VideoKind.EPISODE -> listOfNotNull(video.seriesName, video.seasonEpisodeLabel)
        VideoKind.SERIES -> listOfNotNull(video.yearLabel.ifEmpty { null }, "Series")
        VideoKind.MOVIE -> listOfNotNull(
            video.yearLabel.ifEmpty { null },
            video.runtimeLabel.ifEmpty { null },
        )
    }.joinToString(" · ")
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Artwork(
                video.artColor,
                video.posterUrl,
                Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                corner = 12.dp,
            )
            WatchBadge(video, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Text(
            video.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
    }
}

/**
 * Corner badge summarising watch state: a tick for a finished title, or the number of unwatched
 * episodes for a series part-way through. Renders nothing when there is neither — an untouched
 * title should look untouched, not carry a "0".
 */
@Composable
private fun WatchBadge(video: VideoItem, modifier: Modifier = Modifier) {
    val unplayed = if (video.kind == VideoKind.SERIES) video.unplayedCount else 0
    when {
        video.played -> Box(
            modifier = modifier.clip(CircleShape).background(Accent).padding(4.dp),
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Watched",
                tint = AccentInk,
                modifier = Modifier.size(12.dp),
            )
        }

        unplayed > 0 -> Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Accent)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                unplayed.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentInk,
            )
        }
    }
}
