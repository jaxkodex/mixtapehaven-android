package pe.net.libre.mixtapehaven.ui.nowplaying

import android.media.audiofx.Visualizer
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.ui.theme.AccentGlow
import pe.net.libre.mixtapehaven.ui.theme.AccentPrimary
import kotlin.math.sqrt

private const val TAG = "SpectrumVisualizer"
private const val BAR_COUNT = 32
private const val CAPTURE_SIZE = 1024

@Composable
fun SpectrumVisualizer(
    audioSessionId: Int,
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

    var barValues by remember { mutableStateOf(FloatArray(BAR_COUNT)) }

    DisposableEffect(audioSessionId) {
        val visualizer = createVisualizer(audioSessionId) { fft ->
            barValues = processFft(fft)
        }
        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    val targetValues = if (isPlaying) barValues else FloatArray(BAR_COUNT)

    val animatedBars = FloatArray(BAR_COUNT) { i ->
        animateFloatAsState(
            targetValue = targetValues.getOrElse(i) { 0f },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "bar_$i"
        ).value
    }

    VisualizerCanvas(animatedBars = animatedBars, modifier = modifier)
}

private fun createVisualizer(
    audioSessionId: Int,
    onFftData: (ByteArray) -> Unit
): Visualizer? {
    if (audioSessionId == 0) return null
    return try {
        Visualizer(audioSessionId).apply {
            captureSize = CAPTURE_SIZE
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        v: Visualizer, waveform: ByteArray, samplingRate: Int
                    ) { /* waveform disabled */ }

                    override fun onFftDataCapture(
                        v: Visualizer, fft: ByteArray, samplingRate: Int
                    ) {
                        onFftData(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                /* waveform = */ false,
                /* fft = */ true
            )
            enabled = true
        }
    } catch (e: IllegalStateException) {
        Log.w(TAG, "Visualizer unavailable for session $audioSessionId", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Invalid audio session $audioSessionId", e)
        null
    }
}

private fun processFft(fft: ByteArray): FloatArray {
    val magnitudes = FloatArray(BAR_COUNT)
    val binsPerBar = (fft.size / 2) / BAR_COUNT
    for (bar in 0 until BAR_COUNT) {
        var sum = 0f
        val startBin = bar * binsPerBar + 1 // skip DC (bin 0)
        val endBin = startBin + binsPerBar
        for (k in startBin until endBin) {
            if (k * 2 + 1 < fft.size) {
                val re = fft[k * 2].toFloat()
                val im = fft[k * 2 + 1].toFloat()
                sum += sqrt(re * re + im * im)
            }
        }
        magnitudes[bar] = (sum / binsPerBar / 128f).coerceIn(0f, 1f)
    }
    return magnitudes
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
