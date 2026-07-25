package pe.net.libre.mixtapehaven.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.data.download.DownloadProgress
import pe.net.libre.mixtapehaven.data.download.VideoDownloadStatus
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.ui.components.Artwork
import pe.net.libre.mixtapehaven.ui.components.BackTopBar
import pe.net.libre.mixtapehaven.ui.components.SectionLabel
import pe.net.libre.mixtapehaven.ui.components.StorageBar
import pe.net.libre.mixtapehaven.ui.components.TrackRow
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.Coral
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/** Legend/progress color for the video segment of the storage bar (matches the Online pill green). */
private val VideoLegend = Color(0xFF7BB661)

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel {
        DownloadsViewModel(it.downloadManager, it.videoDownloadManager, it.downloadSettingsStore)
    }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackTopBar(title = "Downloads", onBack = onBack)

        StorageSummary(state)

        state.downloading?.let { AudioDownloadingSection(it) }

        SavedTracksSection(state.saved)

        if (state.videos.isNotEmpty()) {
            SectionLabel("MOVIES & SHOWS")
            Column {
                state.videos.forEach { video ->
                    SavedVideoRow(
                        video = video,
                        onPlay = { onPlayVideo(video.id) },
                        onRetry = { viewModel.retryVideo(video.id) },
                        onRemove = { viewModel.removeVideo(video.id) },
                    )
                }
            }
        }

        // Destructive: remove all downloads
        if (state.saved.isNotEmpty() || state.downloading != null || state.videos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Coral, RoundedCornerShape(14.dp))
                    .clickable(onClick = viewModel::removeAll)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Remove all downloads",
                    style = MaterialTheme.typography.titleMedium,
                    color = Coral,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Combined storage bar (music + video segments) with total and per-type legends. */
@Composable
private fun StorageSummary(state: DownloadsViewModel.UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mixtape downloads", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(state.totalLabel, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        }
        StorageBar(segments = listOf(state.audioFraction to Accent, state.videoFraction to VideoLegend))
        LegendItem(Accent, "Music, saved as you listen · ${state.audioTotalLabel}")
        LegendItem(VideoLegend, "Movies & shows · ${state.videoTotalLabel}")
    }
}

/** The one audio track currently being auto-saved, with its live percent. */
@Composable
private fun AudioDownloadingSection(downloading: DownloadProgress) {
    SectionLabel("DOWNLOADING")
    TrackRow(
        track = downloading.track,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${downloading.percent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                )
                CircularProgressIndicator(
                    progress = { downloading.percent / 100f },
                    color = Accent,
                    trackColor = Surface2,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
    )
}

/** The saved audio library, or the how-it-works hint while it is still empty. */
@Composable
private fun SavedTracksSection(saved: List<Track>) {
    SectionLabel("SAVED ON DEVICE")
    if (saved.isEmpty()) {
        Text(
            "Nothing saved yet. Play anything and it's kept for offline automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    } else {
        Column {
            saved.forEach { track ->
                TrackRow(
                    track = track,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                track.sizeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                contentDescription = "Saved offline",
                                tint = Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * One saved, queued, downloading, or failed video: poster thumb, title/meta, per-state trailing
 * control. Tapping a completed row plays it — the only path to downloaded videos when offline,
 * where the server-backed Home rail and detail screen are unavailable. Failed rows show a retry
 * button; queued rows say why they're waiting via [SavedVideoUi.sizeLabel].
 */
@Composable
private fun SavedVideoRow(
    video: SavedVideoUi,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val complete = video.status == VideoDownloadStatus.COMPLETE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = complete, onClick = onPlay)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Artwork(
            color = video.artColor,
            imageUrl = video.posterUrl,
            modifier = Modifier.size(44.dp),
            corner = 8.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                video.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOf(video.subtitle, video.sizeLabel).filter { it.isNotEmpty() }.joinToString(" · ")
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = if (video.status == VideoDownloadStatus.FAILED) Coral else TextMuted,
            )
        }
        VideoRowStateControl(video.status, onRetry)
        Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = if (complete) "Remove download" else "Cancel download",
            tint = TextSecondary,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(7.dp),
        )
    }
}

/** Spinner while downloading, retry button after a failure; nothing for queued/complete rows. */
@Composable
private fun VideoRowStateControl(status: VideoDownloadStatus, onRetry: () -> Unit) {
    when (status) {
        VideoDownloadStatus.RUNNING -> CircularProgressIndicator(
            color = VideoLegend,
            trackColor = Surface2,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
        )
        VideoDownloadStatus.FAILED -> Icon(
            Icons.Outlined.Refresh,
            contentDescription = "Retry download",
            tint = Accent,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onRetry)
                .padding(7.dp),
        )
        VideoDownloadStatus.QUEUED, VideoDownloadStatus.COMPLETE -> Unit
    }
}

@Composable
private fun LegendItem(dotColor: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
