package pe.net.libre.mixtapehaven.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackManager: PlaybackManager,
    onNavigateBack: () -> Unit
) {
    val viewModel: NowPlayingViewModel = viewModel {
        NowPlayingViewModel(
            playbackManager = playbackManager,
            onNavigateBack = onNavigateBack
        )
    }
    val playbackState by viewModel.playbackState.collectAsState()
    val song = playbackState.currentSong

    // Local state for seeking to prevent UI jitter during drag
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isSeeking) seekPosition else playbackState.progress

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.titleMedium,
                        color = LunarWhite,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = CyberNeonBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack
                )
            )
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        if (song != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Album artwork
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepSpaceBlack),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumCoverUrl != null) {
                        AsyncImage(
                            model = song.albumCoverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = song.albumCoverPlaceholder,
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
                        )
                    }

                    // Show buffering indicator when loading
                    if (playbackState.isBuffering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeepSpaceBlack.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CyberNeonBlue,
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 4.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Song info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = LunarWhite,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LunarWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Progress section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Progress bar - show indeterminate progress when buffering
                    if (playbackState.isBuffering) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyberNeonBlue,
                            trackColor = LunarWhite.copy(alpha = 0.2f)
                        )
                    } else {
                        Slider(
                            value = displayProgress,
                            onValueChange = { newValue ->
                                isSeeking = true
                                seekPosition = newValue  // Update local state for smooth visual feedback
                            },
                            onValueChangeFinished = {
                                // Perform actual seek only when user releases slider
                                if (playbackState.duration > 0) {
                                    val targetPositionMs = (seekPosition * playbackState.duration)
                                        .toLong()
                                        .coerceIn(0L, playbackState.duration)
                                    viewModel.onSeek(targetPositionMs)
                                }
                                isSeeking = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            enabled = playbackState.duration > 0 && playbackState.currentSong != null && !playbackState.isBuffering,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isSeeking) CyberNeonBlue else CyberNeonBlue.copy(alpha = 0f),
                                activeTrackColor = CyberNeonBlue,
                                inactiveTrackColor = LunarWhite.copy(alpha = 0.2f),
                                disabledThumbColor = LunarWhite.copy(alpha = 0f),
                                disabledActiveTrackColor = CyberNeonBlue.copy(alpha = 0.3f),
                                disabledInactiveTrackColor = LunarWhite.copy(alpha = 0.1f)
                            ),
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp)),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = CyberNeonBlue,
                                        inactiveTrackColor = LunarWhite.copy(alpha = 0.2f)
                                    )
                                )
                            },
                            thumb = {
                                if (isSeeking) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(CyberNeonBlue, CircleShape)
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time stamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playbackState.currentPositionFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = LunarWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = playbackState.durationFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = LunarWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    IconButton(
                        onClick = { viewModel.onPreviousClick() },
                        modifier = Modifier.size(64.dp),
                        enabled = !playbackState.isBuffering && playbackState.hasPrevious
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (playbackState.isBuffering || !playbackState.hasPrevious)
                                LunarWhite.copy(alpha = 0.3f) else LunarWhite,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Play/Pause button
                    IconButton(
                        onClick = { viewModel.onPlayPauseClick() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                if (playbackState.isBuffering) CyberNeonBlue.copy(alpha = 0.5f) else CyberNeonBlue,
                                CircleShape
                            ),
                        enabled = !playbackState.isBuffering
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = if (playbackState.isBuffering) DeepSpaceBlack.copy(alpha = 0.5f) else DeepSpaceBlack,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Next button
                    IconButton(
                        onClick = { viewModel.onNextClick() },
                        modifier = Modifier.size(64.dp),
                        enabled = !playbackState.isBuffering && playbackState.hasNext
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (playbackState.isBuffering || !playbackState.hasNext)
                                LunarWhite.copy(alpha = 0.3f) else LunarWhite,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            // No song playing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No song playing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LunarWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}
