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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import pe.net.libre.mixtapehaven.data.playback.PlaybackState
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackManager: PlaybackManager,
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: NowPlayingViewModel = viewModel {
        NowPlayingViewModel(
            playbackManager = playbackManager,
            mediaRepository = mediaRepository,
            onNavigateBack = onNavigateBack
        )
    }
    val playbackState by viewModel.playbackState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
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

                NowPlayingAlbumArt(
                    albumCoverUrl = song.albumCoverUrl,
                    albumCoverPlaceholder = song.albumCoverPlaceholder,
                    songTitle = song.title,
                    isBuffering = playbackState.isBuffering
                )

                Spacer(modifier = Modifier.height(48.dp))

                NowPlayingSongInfo(
                    title = song.title,
                    artist = song.artist
                )

                Spacer(modifier = Modifier.height(48.dp))

                NowPlayingProgressBar(
                    playbackState = playbackState,
                    isSeeking = isSeeking,
                    displayProgress = displayProgress,
                    onSeekStart = { newValue ->
                        isSeeking = true
                        seekPosition = newValue
                    },
                    onSeekFinished = {
                        if (playbackState.duration > 0) {
                            val targetPositionMs = (seekPosition * playbackState.duration)
                                .toLong()
                                .coerceIn(0L, playbackState.duration)
                            viewModel.onSeek(targetPositionMs)
                        }
                        isSeeking = false
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                NowPlayingControls(
                    playbackState = playbackState,
                    onPreviousClick = { viewModel.onPreviousClick() },
                    onPlayPauseClick = { viewModel.onPlayPauseClick() },
                    onNextClick = { viewModel.onNextClick() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                NowPlayingInstantMix(
                    isLoadingMix = uiState.isLoadingMix,
                    onInstantMix = { viewModel.startInstantMix() }
                )

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

@Composable
private fun NowPlayingAlbumArt(
    albumCoverUrl: String?,
    albumCoverPlaceholder: String,
    songTitle: String,
    isBuffering: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSpaceBlack),
        contentAlignment = Alignment.Center
    ) {
        if (albumCoverUrl != null) {
            AsyncImage(
                model = albumCoverUrl,
                contentDescription = songTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = albumCoverPlaceholder,
                style = MaterialTheme.typography.displayLarge,
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
            )
        }

        if (isBuffering) {
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
}

@Composable
private fun NowPlayingSongInfo(
    title: String,
    artist: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = LunarWhite,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = LunarWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingProgressBar(
    playbackState: PlaybackState,
    isSeeking: Boolean,
    displayProgress: Float,
    onSeekStart: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                onValueChange = onSeekStart,
                onValueChangeFinished = onSeekFinished,
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
}

@Composable
private fun NowPlayingControls(
    playbackState: PlaybackState,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
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

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (playbackState.isBuffering) CyberNeonBlue.copy(alpha = 0.5f) else CyberNeonBlue,
                    CircleShape
                ),
            enabled = !playbackState.isBuffering
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                tint = if (playbackState.isBuffering) DeepSpaceBlack.copy(alpha = 0.5f) else DeepSpaceBlack,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        IconButton(
            onClick = onNextClick,
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
}

@Composable
private fun NowPlayingInstantMix(
    isLoadingMix: Boolean,
    onInstantMix: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onInstantMix,
        modifier = modifier.size(48.dp),
        enabled = !isLoadingMix
    ) {
        if (isLoadingMix) {
            CircularProgressIndicator(
                color = CyberNeonBlue,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Instant Mix",
                tint = CyberNeonBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
