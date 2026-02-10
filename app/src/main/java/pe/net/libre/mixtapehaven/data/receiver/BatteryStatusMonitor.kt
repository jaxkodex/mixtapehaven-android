package pe.net.libre.mixtapehaven.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager

/**
 * Monitors battery level and pauses/resumes downloads accordingly
 */
class BatteryStatusMonitor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BatteryStatusMonitor"
        const val DEFAULT_BATTERY_THRESHOLD = 20
    }

    private val dataStoreManager = DataStoreManager(context)
    private val database = OfflineDatabase.getInstance(context)
    private val workManager = DownloadWorkManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _shouldPauseDownloads = MutableStateFlow(false)
    val shouldPauseDownloads: StateFlow<Boolean> = _shouldPauseDownloads

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            _batteryLevel.value = batteryPct
            _isCharging.value = charging

            checkBatteryAndUpdateDownloads(batteryPct, charging)
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(batteryReceiver, filter)

        // Check initial battery state
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
            _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        scope.cancel()
    }

    private fun checkBatteryAndUpdateDownloads(batteryPct: Int, isCharging: Boolean) {
        scope.launch {
            val threshold = dataStoreManager.batteryThreshold.first() ?: DEFAULT_BATTERY_THRESHOLD

            val shouldPause = batteryPct < threshold && !isCharging
            _shouldPauseDownloads.value = shouldPause

            if (shouldPause) {
                Log.d(TAG, "Battery low ($batteryPct% < $threshold%) and not charging. Pausing downloads.")
                pauseActiveDownloads()
            } else {
                Log.d(TAG, "Battery OK ($batteryPct%, charging: $isCharging). Resuming downloads if needed.")
                resumePausedDownloads()
            }
        }
    }

    private suspend fun pauseActiveDownloads() {
        // Get all downloading playlists
        val downloadingPlaylists = database.downloadedPlaylistDao()
            .getPlaylistsByStatus(PlaylistDownloadStatus.DOWNLOADING)

        downloadingPlaylists.collect { playlists ->
            playlists.forEach { playlist ->
                // Update status to PAUSED
                database.downloadedPlaylistDao().updatePlaylistStatus(
                    playlist.playlistId,
                    PlaylistDownloadStatus.PAUSED
                )
                // Cancel the work
                workManager.pausePlaylistDownload(playlist.playlistId)
            }
        }
    }

    private suspend fun resumePausedDownloads() {
        // Get all paused playlists
        val pausedPlaylists = database.downloadedPlaylistDao()
            .getPlaylistsByStatus(PlaylistDownloadStatus.PAUSED)

        pausedPlaylists.collect { playlists ->
            playlists.forEach { playlist ->
                // Update status back to DOWNLOADING
                database.downloadedPlaylistDao().updatePlaylistStatus(
                    playlist.playlistId,
                    PlaylistDownloadStatus.DOWNLOADING
                )
                // Resume download
                resumePlaylistDownload(playlist.playlistId, playlist.name)
            }
        }
    }

    private suspend fun resumePlaylistDownload(playlistId: String, playlistName: String) {
        // Get pending/failed songs for this playlist
        val crossRefs = database.playlistSongCrossRefDao()
            .getSongsByStatus(playlistId, pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus.PENDING) +
                database.playlistSongCrossRefDao()
                    .getSongsByStatus(playlistId, pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus.FAILED)

        if (crossRefs.isEmpty()) return

        val songIds = crossRefs.map { it.songId }
        val songTitles = crossRefs.map { "Song" } // We don't have titles in crossRef, would need to fetch
        val fileSizes = crossRefs.map { it.fileSize }

        // Get quality preference
        val quality = dataStoreManager.downloadQuality.first().name ?: "ORIGINAL"

        // Get playlist info
        val playlist = database.downloadedPlaylistDao().getPlaylistById(playlistId)

        workManager.resumePlaylistDownload(
            playlistId = playlistId,
            playlistName = playlistName,
            remainingSongIds = songIds,
            remainingSongTitles = songTitles,
            remainingFileSizes = fileSizes,
            quality = quality,
            coverUrl = playlist?.coverUrl
        )
    }
}
