package pe.net.libre.mixtapehaven.data.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

/**
 * BroadcastReceiver that monitors Bluetooth and audio output disconnection events.
 * Pauses playback when:
 * - Bluetooth headphones disconnect
 * - Wired headphones are unplugged (via ACTION_AUDIO_BECOMING_NOISY)
 */
class BluetoothDisconnectionReceiver(
    private val playbackManager: PlaybackManager
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // Bluetooth device disconnected
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Bluetooth device disconnected: ${device?.name}")
                
                // Pause playback if currently playing
                if (playbackManager.playbackState.value.isPlaying) {
                    Log.d(TAG, "Pausing playback due to Bluetooth disconnection")
                    playbackManager.pause()
                }
            }
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                // Audio output became noisy (e.g., headphones unplugged)
                Log.d(TAG, "Audio became noisy (headphones unplugged)")
                
                // Pause playback if currently playing
                if (playbackManager.playbackState.value.isPlaying) {
                    Log.d(TAG, "Pausing playback due to audio becoming noisy")
                    playbackManager.pause()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothDisconnectReceiver"
    }
}
