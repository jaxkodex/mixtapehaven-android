package pe.net.libre.mixtapehaven.data.playback

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

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

                // Log device name only if we have permission
                val deviceName = if (context != null && hasBluetoothConnectPermission(context)) {
                    device?.name ?: "Unknown"
                } else {
                    "Unknown (permission not granted)"
                }
                Log.d(TAG, "Bluetooth device disconnected: $deviceName")

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

    /**
     * Checks if the app has BLUETOOTH_CONNECT permission (required on Android 12+)
     */
    private fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission not required on Android 11 and below
            true
        }
    }

    companion object {
        private const val TAG = "BluetoothDisconnectReceiver"
    }
}
