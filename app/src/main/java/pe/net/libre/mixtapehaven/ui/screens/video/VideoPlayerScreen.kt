package pe.net.libre.mixtapehaven.ui.screens.video

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary

/** Full-screen video playback surface using media3's [PlayerView] for transport controls. */
// PlayerView and its show/hide-button setters are @UnstableApi; media3 requires an explicit opt-in.
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel = appViewModel {
        VideoPlayerViewModel(
            appContext,
            it.repository,
            it.videoProgressStore,
            it.playerController,
            itemId,
            it.videoSourceResolver,
            it.serverAvailability,
        )
    }
    val error by viewModel.error.collectAsState()
    val upNext by viewModel.upNext.collectAsState()
    val buffering by viewModel.buffering.collectAsState()
    val playbackActive by viewModel.playbackActive.collectAsState()

    PauseWhenScreenStops(viewModel::onScreenStopped)
    LandscapeWhilePlaying(LocalActivity.current)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.player
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            // Awake while playing, and across the load: the player is STATE_IDLE until prepare(),
            // so playbackActive alone would let the display sleep through the resolve hop — and
            // the ON_STOP observer above turns that into a pause the user never asked for.
            //
            // What this stops is the display being pinned on for as long as the screen is
            // composed: behind a paused video, or a "Playback failed" message, both of which can
            // sit there for hours. A stream stuck buffering still holds it, deliberately — a
            // rebuffer must not blank the picture — but that ends when the load times out into an
            // error, which releases it.
            update = { it.keepScreenOn = playbackActive || buffering },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        // Suppressed once an error is up: the message explains the black frame better than a
        // spinner that would never resolve.
        if (buffering && error == null) {
            BufferingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        error?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))

        upNext?.let { next ->
            NextEpisodePill(
                label = next.seasonEpisodeLabel ?: "Next episode",
                onClick = viewModel::playNext,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Pause playback once the screen stops — Home button, screen lock, or navigating away.
 *
 * There is no background video service, so without this the audio keeps playing invisibly behind
 * whatever the user moved on to.
 *
 * The observer is keyed on the lifecycle owner alone and reads [onScreenStopped] through
 * [rememberUpdatedState], so a recomposition that hands in a fresh lambda swaps the callback
 * instead of tearing the observer down and re-registering it.
 */
@Composable
private fun PauseWhenScreenStops(onScreenStopped: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // Read through .value rather than a `by` delegate. Both are the same read; the delegate is
    // what static analysis flags as an unused variable, because the only use is a getValue call
    // inside the observer lambda below and that is a hop it does not follow.
    val currentCallback = rememberUpdatedState(onScreenStopped)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentCallback.value()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Holds the window in landscape — either side, following the sensor — for as long as the player is
 * composed, and hands orientation back to the system on the way out.
 *
 * SENSOR_LANDSCAPE is deliberate over plain LANDSCAPE: the sensor picks either side, so the picture
 * stays upright however the phone is turned, and it overrides the system rotation lock, so starting
 * a video must not begin with a detour into Quick Settings to enable auto-rotate. The restore to
 * UNSPECIFIED on dispose puts every other screen back under the user's own setting.
 *
 * The flip itself never recreates the activity: MainActivity declares the orientation
 * configChanges (see the manifest), so the request is answered with an in-place relayout and the
 * stream never stutters.
 */
@Composable
private fun LandscapeWhilePlaying(activity: Activity?) {
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/** Spinner shown over the black frame while a stream is being resolved or buffered. */
@Composable
private fun BufferingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = Accent,
        // Without this the load is silent to TalkBack — the black frame with no announcement is
        // the exact state this indicator exists to explain.
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = "Loading video" },
    )
}

/** Circular back affordance over the video surface, which has no system chrome of its own. */
@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(12.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
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

/**
 * Skip-to-next-episode pill.
 *
 * A dedicated control rather than PlayerView's next button: the player holds a single MediaItem at
 * a time (each episode negotiates its own stream), so the built-in next would render permanently
 * disabled.
 */
@Composable
private fun NextEpisodePill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(12.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Icon(
            Icons.Filled.SkipNext,
            contentDescription = "Play next episode",
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
