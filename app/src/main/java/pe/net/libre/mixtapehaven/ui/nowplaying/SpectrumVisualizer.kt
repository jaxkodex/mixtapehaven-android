package pe.net.libre.mixtapehaven.ui.nowplaying

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pe.net.libre.mixtapehaven.ui.theme.AccentGlow
import pe.net.libre.mixtapehaven.ui.theme.AccentPrimary
import kotlin.random.Random

private const val BAR_COUNT = 32
private const val UPDATE_INTERVAL_MS = 100L

@Composable
fun SpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Respect system Reduce Motion setting
    val animatorScale = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    if (animatorScale == 0f) return

    var targetBars by remember { mutableStateOf(FloatArray(BAR_COUNT)) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            targetBars = FloatArray(BAR_COUNT)
            return@LaunchedEffect
        }
        while (true) {
            targetBars = FloatArray(BAR_COUNT) { i -> simulatedBarValue(i) }
            delay(UPDATE_INTERVAL_MS)
        }
    }

    val animatedBars = FloatArray(BAR_COUNT) { i ->
        animateFloatAsState(
            targetValue = targetBars.getOrElse(i) { 0f },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "bar_$i"
        ).value
    }

    VisualizerCanvas(animatedBars = animatedBars, modifier = modifier)
}

/**
 * Returns a random bar height shaped to mimic a typical music spectrum:
 * most energy in the bass/low-mid range, rolling off at the high end.
 */
private fun simulatedBarValue(barIndex: Int): Float {
    val (min, max) = when {
        barIndex < 4  -> 0.30f to 0.80f  // sub-bass
        barIndex < 12 -> 0.50f to 1.00f  // bass / low-mid (most energy)
        barIndex < 20 -> 0.30f to 0.70f  // mid
        barIndex < 28 -> 0.10f to 0.50f  // high-mid
        else          -> 0.05f to 0.25f  // highs
    }
    return min + Random.nextFloat() * (max - min)
}

@Composable
private fun VisualizerCanvas(
    animatedBars: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val barWidth = size.width / BAR_COUNT
        val gap = barWidth * 0.2f
        val effectiveBarWidth = barWidth - gap

        for (i in 0 until BAR_COUNT) {
            val barHeight = animatedBars[i] * size.height
            val left = i * barWidth + gap / 2f
            val top = size.height - barHeight

            // Bloom rect (slightly wider, AccentGlow at 20% opacity)
            drawRect(
                color = AccentGlow.copy(alpha = 0.20f),
                topLeft = androidx.compose.ui.geometry.Offset(left - gap / 2f, top),
                size = androidx.compose.ui.geometry.Size(effectiveBarWidth + gap, barHeight)
            )

            // Main bar
            drawRect(
                color = AccentPrimary,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(effectiveBarWidth, barHeight)
            )
        }
    }
}
