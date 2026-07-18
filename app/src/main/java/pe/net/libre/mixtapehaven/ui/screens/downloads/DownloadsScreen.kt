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
import pe.net.libre.mixtapehaven.di.appViewModel
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
fun DownloadsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = appViewModel { DownloadsViewModel(it.downloadManager, it.videoDownloadManager) }
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

        // Storage summary block
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

        val downloading = state.downloading
        if (downloading != null) {
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

        SectionLabel("SAVED ON DEVICE")

        if (state.saved.isEmpty()) {
            Text(
                "Nothing saved yet. Play anything and it's kept for offline automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            Column {
                state.saved.forEach { track ->
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

        if (state.videos.isNotEmpty()) {
            SectionLabel("MOVIES & SHOWS")
            Column {
                state.videos.forEach { video ->
                    SavedVideoRow(
                        video = video,
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

/** One saved (or still-downloading) video: poster thumb, title/meta, and cancel/delete control. */
@Composable
private fun SavedVideoRow(video: SavedVideoUi, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
            Text(meta, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        if (video.downloading) {
            CircularProgressIndicator(
                color = VideoLegend,
                trackColor = Surface2,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        }
        Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = if (video.downloading) "Cancel download" else "Remove download",
            tint = TextSecondary,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(7.dp),
        )
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
