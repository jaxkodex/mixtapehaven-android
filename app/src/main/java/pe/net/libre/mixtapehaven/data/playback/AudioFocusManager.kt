package pe.net.libre.mixtapehaven.data.playback

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Manages audio focus for the music player.
 * Handles requesting, abandoning, and responding to audio focus changes.
 *
 * This ensures proper behavior when:
 * - Phone calls enter (permanent focus loss)
 * - Other media apps start playing (transient focus loss)
 * - Notification sounds play (transient can duck)
 */
class AudioFocusManager(
    private val context: Context,
    private val onAudioFocusLost: () -> Unit,
    private val onAudioFocusGained: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wasPlayingWhenLost = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - stop playback
                // This happens when another app takes permanent focus (e.g., phone call, another music app)
                Log.d(TAG, "Audio focus lost permanently")
                onAudioFocusLost()
                wasPlayingWhenLost = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - pause playback
                // This happens for short interruptions like navigation voice, notifications
                Log.d(TAG, "Audio focus lost transiently")
                wasPlayingWhenLost = true
                onAudioFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Optional: Lower volume instead of pausing
                // For now, we'll pause for consistency with user expectations
                Log.d(TAG, "Audio focus lost transiently (can duck)")
                wasPlayingWhenLost = true
                onAudioFocusLost()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus regained - resume if we were playing
                Log.d(TAG, "Audio focus regained")
                if (wasPlayingWhenLost) {
                    onAudioFocusGained()
                }
            }
        }
    }

    /**
     * Request audio focus with AUDIOFOCUS_GAIN.
     * Should be called before starting playback.
     *
     * @return true if audio focus was granted, false otherwise
     */
    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            focusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.d(TAG, "Audio focus request result: $granted")
        return granted
    }

    /**
     * Abandon audio focus.
     * Should be called when stopping playback or when the service is destroyed.
     */
    fun abandonAudioFocus() {
        val result = audioManager.abandonAudioFocus(focusChangeListener)
        Log.d(TAG, "Audio focus abandon result: $result")
        wasPlayingWhenLost = false
    }

    companion object {
        private const val TAG = "AudioFocusManager"
    }
}
