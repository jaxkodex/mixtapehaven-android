package pe.net.libre.mixtapehaven.ui.screens.video

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import pe.net.libre.mixtapehaven.ui.theme.Stroke
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
    val viewModel = appViewModel { VideoDetailViewModel(it.repository, it.videoDownloadManager, itemId) }
    val state by viewModel.state.collectAsState()
    val downloadUi by viewModel.downloadUi.collectAsState()

    // Downloads fail silently in the manager (logcat only); give the user the reason here.
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.downloadErrors.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
                seasons = state.seasons.map { it.first },
                selectedSeason = state.selectedSeason,
                onSelectSeason = viewModel::selectSeason,
                episodes = state.visibleEpisodes,
                downloadUi = downloadUi,
                onPlay = { viewModel.playTarget()?.let { onPlay(it.id) } },
                onPlayEpisode = { onPlay(it.id) },
                onDownload = viewModel::download,
                onRemoveDownload = viewModel::removeDownload,
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
    seasons: List<String>,
    selectedSeason: String?,
    onSelectSeason: (String) -> Unit,
    episodes: List<VideoItem>,
    downloadUi: VideoDownloadUi,
    onPlay: () -> Unit,
    onPlayEpisode: (VideoItem) -> Unit,
    onDownload: (VideoItem) -> Unit,
    onRemoveDownload: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TitleBlock(item)

        PlayButton(
            label = if (item.kind != VideoKind.SERIES && item.resumePositionMs > 0) "Resume" else "Play",
            onClick = onPlay,
        )

        // Series are downloaded per-episode via the row icons; movies/episodes get a single button.
        if (item.kind != VideoKind.SERIES) {
            DownloadButton(
                item = item,
                downloadUi = downloadUi,
                onDownload = onDownload,
                onRemoveDownload = onRemoveDownload,
            )
        }

        if (item.overview.isNotEmpty()) {
            Text(item.overview, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        if (episodes.isNotEmpty()) {
            // A single-season show gets the plain "Episodes" heading: a lone season chip is a
            // control the user can never change anything with.
            if (seasons.size > 1) {
                SeasonSelector(
                    seasons = seasons,
                    selected = selectedSeason ?: seasons.first(),
                    onSelect = onSelectSeason,
                )
            } else {
                Text("Episodes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Column {
                episodes.forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        downloadUi = downloadUi,
                        onClick = { onPlayEpisode(episode) },
                        onDownload = onDownload,
                        onRemoveDownload = onRemoveDownload,
                    )
                }
            }
        }
    }
}

/** Display title with the "1931 · 1h 14m · PG · ★ 7.8" meta line and genres under it. */
@Composable
private fun TitleBlock(item: VideoItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(item.title, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        val meta = listOfNotNull(
            item.yearLabel.ifEmpty { null },
            when (item.kind) {
                VideoKind.SERIES -> "Series"
                else -> item.runtimeLabel.ifEmpty { null }
            },
            item.officialRating.ifEmpty { null },
            item.communityRating?.let { "★ %.1f".format(it) },
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (item.genres.isNotEmpty()) {
            Text(
                item.genres.take(MAX_GENRES).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

/** Genres past this are noise on a phone-width line. */
private const val MAX_GENRES = 3

/** Horizontally scrolling season chips for a multi-season series. */
@Composable
private fun SeasonSelector(
    seasons: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(seasons, key = { it }) { season ->
            val isSelected = season == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onSelect(season) }
                    .background(if (isSelected) Accent else Surface2, CircleShape)
                    .border(1.dp, if (isSelected) Accent else Stroke, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    season,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) AccentInk else TextSecondary,
                )
            }
        }
    }
}

/**
 * Secondary full-width control cycling through the download lifecycle: Download -> transcoding
 * progress (tap cancels) -> Downloaded (tap removes).
 */
@Composable
private fun DownloadButton(
    item: VideoItem,
    downloadUi: VideoDownloadUi,
    onDownload: (VideoItem) -> Unit,
    onRemoveDownload: (String) -> Unit,
) {
    val inFlightLabel = downloadUi.inFlightLabels[item.id]
    val downloaded = item.id in downloadUi.downloadedIds
    val (label, onClick) = when {
        downloaded -> "Downloaded · Remove" to { onRemoveDownload(item.id) }
        inFlightLabel != null -> "Downloading · $inFlightLabel · Cancel" to { onRemoveDownload(item.id) }
        else -> "Download" to { onDownload(item) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            downloaded -> Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp),
            )
            inFlightLabel != null -> CircularProgressIndicator(
                color = Accent,
                trackColor = Surface2,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            else -> Icon(
                Icons.Outlined.ArrowDownward,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
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
        if (episode.played) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .padding(3.dp),
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Watched",
                    tint = AccentInk,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
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

/** One episode: 16:9 still, title, "S2 E4 · 42m" meta, download state control, and play affordance. */
@Composable
private fun EpisodeRow(
    episode: VideoItem,
    downloadUi: VideoDownloadUi,
    onClick: () -> Unit,
    onDownload: (VideoItem) -> Unit,
    onRemoveDownload: (String) -> Unit,
) {
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
        EpisodeDownloadIcon(episode, downloadUi, onDownload, onRemoveDownload)
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play episode",
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Download state for one episode: arrow (download), spinner (cancel), or check (remove). */
@Composable
private fun EpisodeDownloadIcon(
    episode: VideoItem,
    downloadUi: VideoDownloadUi,
    onDownload: (VideoItem) -> Unit,
    onRemoveDownload: (String) -> Unit,
) {
    val inFlight = episode.id in downloadUi.inFlightLabels
    val downloaded = episode.id in downloadUi.downloadedIds
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) {
                if (downloaded || inFlight) onRemoveDownload(episode.id) else onDownload(episode)
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            downloaded -> Icon(
                Icons.Outlined.Check,
                contentDescription = "Remove download",
                tint = Accent,
                modifier = Modifier.size(20.dp),
            )
            inFlight -> CircularProgressIndicator(
                color = Accent,
                trackColor = Surface2,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            else -> Icon(
                Icons.Outlined.ArrowDownward,
                contentDescription = "Download episode",
                tint = TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
