package pe.net.libre.mixtapehaven.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.components.Artwork
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Bg
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun NowPlayingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = appViewModel { NowPlayingViewModel(it.playerController) }
    val track by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Close",
                tint = TextPrimary,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PLAYING FROM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Your library",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    textAlign = TextAlign.Center,
                )
            }
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = "More",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }

        // Hero album art
        Artwork(
            color = track?.artColor ?: Surface2,
            imageUrl = track?.imageUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 8.dp),
            corner = 20.dp,
        )

        // Track info
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                track?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                track?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }

        // Progress
        Column {
            Slider(
                value = progress,
                onValueChange = viewModel::seekToFraction,
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Surface2,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(positionMs), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Shuffle,
                contentDescription = "Shuffle",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp).clickable(onClick = viewModel::previous),
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable(onClick = viewModel::playPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AccentInk,
                    modifier = Modifier.size(36.dp),
                )
            }
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp).clickable(onClick = viewModel::next),
            )
            Icon(
                Icons.Outlined.Repeat,
                contentDescription = "Repeat",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
