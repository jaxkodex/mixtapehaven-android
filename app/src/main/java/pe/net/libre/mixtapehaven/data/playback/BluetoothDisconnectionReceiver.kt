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
    private val onDisconnect: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                val deviceName = try {
                    device?.name ?: "Unknown"
                } catch (e: SecurityException) {
                    "Unknown (no permission)"
                }
                Log.d(TAG, "Bluetooth device disconnected: $deviceName")
                onDisconnect()
            }
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Log.d(TAG, "Audio became noisy (headphones unplugged)")
                onDisconnect()
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothDisconnectReceiver"
    }
}
