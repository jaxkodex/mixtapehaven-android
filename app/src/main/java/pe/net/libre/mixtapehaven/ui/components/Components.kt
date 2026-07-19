package pe.net.libre.mixtapehaven.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Stroke
import pe.net.libre.mixtapehaven.ui.theme.Surface
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/**
 * Album artwork. Renders the real [imageUrl] when present; otherwise falls back to the
 * concentric-circle "vinyl record" drawing tinted by [color].
 */
@Composable
fun Artwork(
    color: Color,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(corner)).background(color),
        )
    } else {
        VinylArt(color, modifier, corner)
    }
}

/** Concentric-circle "vinyl record" artwork, tinted by [color]. */
@Composable
fun VinylArt(color: Color, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().padding(8.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.55f).compositeOverBlack()),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxSize().padding(10.dp).clip(CircleShape).background(Accent.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AccentInk))
            }
        }
    }
}

private fun Color.compositeOverBlack(): Color =
    Color(red * alpha, green * alpha, blue * alpha, 1f)

@Composable
fun StatusPill(text: String, dotColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, Stroke, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun CircularPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    background: Color = Accent,
    tint: Color = AccentInk,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Accent,
                modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier,
            )
        }
    }
}

/** Read-only when [onClick] is set (Home); editable when [onValueChange] is set (Search). */
@Composable
fun SearchField(
    placeholder: String,
    modifier: Modifier = Modifier,
    value: TextFieldValue = TextFieldValue(""),
    onValueChange: ((TextFieldValue) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowBase = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Surface2)
        .border(1.dp, Stroke, RoundedCornerShape(14.dp))
    Row(
        modifier = if (onClick != null) rowBase.clickable(onClick = onClick) else rowBase,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.padding(start = 14.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 0.dp)) {
            if (onValueChange != null) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth().padding(end = 14.dp),
                )
                if (value.text.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            } else {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val base = modifier.fillMaxWidth()
    Row(
        modifier = if (onClick != null) base.clickable(onClick = onClick).padding(vertical = 8.dp) else base.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Artwork(track.artColor, track.imageUrl, Modifier.size(48.dp), corner = 8.dp)
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            trailing()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(track.durationLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                if (track.downloaded) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Downloaded", tint = Accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Compact bar showing the current [track] with play/pause and skip-next controls.
 * Tapping anywhere outside the controls invokes [onOpen] (navigates to Now Playing).
 */
@Composable
fun NowPlayingBar(
    track: Track,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface)
            .clickable(onClick = onOpen)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Stroke))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The 48.dp minimum touch targets in the controls already add vertical bulk.
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(track.artColor, track.imageUrl, Modifier.size(44.dp), corner = 8.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NowPlayingControls(isPlaying = isPlaying, onPlayPause = onPlayPause, onNext = onNext)
        }
    }
}

/** Play/pause and skip-next controls, each wrapped in a 48.dp minimum touch target. */
@Composable
private fun NowPlayingControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onPlayPause)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = AccentInk,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onNext)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SkipNext,
            contentDescription = "Next track",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun AlbumCard(album: Album, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            Artwork(album.artColor, album.imageUrl, Modifier.fillMaxWidth().aspectRatio(1f), corner = 14.dp)
            if (album.downloaded) {
                Box(
                    Modifier.padding(8.dp).clip(CircleShape).background(AccentInk.copy(alpha = 0.7f)).padding(4.dp),
                ) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Downloaded", tint = Accent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(album.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Portrait 2:3 poster tile for a movie or TV series.
 *
 * The caller sizes it — the Home rail pins a width, the library grid lets the cell decide — so no
 * width is baked in here.
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

@Composable
fun RandomWalkCard(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "RANDOM WALK",
    title: String = "Press play, get on with life",
    subtitle: String = "A shuffled walk through your whole library.",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Accent)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tag, style = MaterialTheme.typography.labelMedium, color = AccentInk.copy(alpha = 0.7f))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = AccentInk)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AccentInk.copy(alpha = 0.75f))
        }
        CircularPlayButton(onClick = onPlay, size = 56.dp, background = AccentInk, tint = Accent)
    }
}

@Composable
fun FilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) Accent else Surface2)
                    .then(if (selected) Modifier else Modifier.border(1.dp, Stroke, CircleShape))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) AccentInk else TextSecondary,
                )
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentInk,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Stroke,
            ),
        )
    }
}

/** Proportional storage usage bar. [segments] are (fraction, color) pairs that should sum to <= 1. */
@Composable
fun StorageBar(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Surface2),
    ) {
        segments.forEach { (fraction, color) ->
            Box(Modifier.weight(fraction.coerceAtLeast(0.0001f)).fillMaxSize().background(color))
        }
        val used = segments.sumOf { it.first.toDouble() }.toFloat()
        if (used < 1f) Spacer(Modifier.weight(1f - used))
    }
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
            modifier = Modifier.size(24.dp).clickable(onClick = onBack),
        )
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelMedium, color = TextMuted)
}

/** Shared surface card wrapper used by several screens. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Stroke, RoundedCornerShape(16.dp)),
    ) { content() }
}

@Suppress("unused")
private val unusedStyle: TextStyle = TextStyle.Default
private val previewTextColor = Color.Unspecified
