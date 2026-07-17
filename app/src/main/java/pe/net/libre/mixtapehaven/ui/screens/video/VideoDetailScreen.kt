package pe.net.libre.mixtapehaven.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import pe.net.libre.mixtapehaven.ui.components.Artwork
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun VideoDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel { VideoDetailViewModel(it.repository, itemId) }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Backdrop(item = state.item, onBack = onBack)

        val item = state.item
        when {
            item != null -> DetailBody(
                item = item,
                episodes = state.episodes,
                onPlay = { viewModel.playTarget()?.let { onPlay(it.id) } },
                onPlayEpisode = { onPlay(it.id) },
            )
            state.loading -> Unit
            else -> Text(
                state.error ?: "Could not load this title",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

/** Wide backdrop image with the back control overlaid, falling back to the tinted poster art. */
@Composable
private fun Backdrop(item: VideoItem?, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Artwork(
            color = item?.artColor ?: Surface2,
            imageUrl = item?.backdropUrl ?: item?.posterUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            corner = 0.dp,
        )
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentInk.copy(alpha = 0.55f))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DetailBody(
    item: VideoItem,
    episodes: List<VideoItem>,
    onPlay: () -> Unit,
    onPlayEpisode: (VideoItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            val meta = listOfNotNull(
                item.yearLabel.ifEmpty { null },
                when (item.kind) {
                    VideoKind.SERIES -> "Series"
                    else -> item.runtimeLabel.ifEmpty { null }
                },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        PlayButton(
            label = if (item.kind != VideoKind.SERIES && item.resumePositionMs > 0) "Resume" else "Play",
            onClick = onPlay,
        )

        if (item.overview.isNotEmpty()) {
            Text(item.overview, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        if (episodes.isNotEmpty()) {
            Text("Episodes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Column {
                episodes.forEach { episode ->
                    EpisodeRow(episode = episode, onClick = { onPlayEpisode(episode) })
                }
            }
        }
    }
}

@Composable
private fun PlayButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = AccentInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AccentInk,
        )
    }
}

/** 16:9 episode still with a resume progress bar overlaid when the episode was started. */
@Composable
private fun EpisodeThumb(episode: VideoItem) {
    Box {
        Artwork(
            color = episode.artColor,
            imageUrl = episode.posterUrl,
            modifier = Modifier.width(112.dp).aspectRatio(16f / 9f),
            corner = 8.dp,
        )
        if (episode.resumePositionMs > 0 && episode.runtimeMs > 0) {
            val fraction = (episode.resumePositionMs.toFloat() / episode.runtimeMs).coerceIn(0f, 1f)
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(AccentInk.copy(alpha = 0.6f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .background(Accent),
                )
            }
        }
    }
}

/** One episode: 16:9 still, title, "S2 E4 · 42m" meta, with a resume progress bar when started. */
@Composable
private fun EpisodeRow(episode: VideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EpisodeThumb(episode)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                episode.seasonEpisodeLabel,
                episode.runtimeLabel.ifEmpty { null },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play episode",
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}
