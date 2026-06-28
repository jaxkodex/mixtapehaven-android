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
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.di.appViewModel
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

@Composable
fun DownloadsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = appViewModel { DownloadsViewModel(it.downloadManager) }
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
            StorageBar(segments = listOf(state.usedFraction to Accent))
            LegendItem(Accent, "Saved as you listen · ${state.totalLabel}")
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

        // Destructive: remove all downloads
        if (state.saved.isNotEmpty() || state.downloading != null) {
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
