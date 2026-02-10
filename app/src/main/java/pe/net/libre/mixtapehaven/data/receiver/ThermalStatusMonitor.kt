package pe.net.libre.mixtapehaven.data.receiver

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager

/**
 * Monitors device thermal status and pauses/resumes downloads accordingly
 */
class ThermalStatusMonitor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ThermalStatusMonitor"
    }

    private val dataStoreManager = DataStoreManager(context)
    private val database = OfflineDatabase.getInstance(context)
    private val workManager = DownloadWorkManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _thermalStatus = MutableStateFlow(PowerManager.THERMAL_STATUS_NONE)
    val thermalStatus: StateFlow<Int> = _thermalStatus

    private val _shouldPauseDownloads = MutableStateFlow(false)
    val shouldPauseDownloads: StateFlow<Boolean> = _shouldPauseDownloads

    private var thermalStatusListener: PowerManager.OnThermalStatusChangedListener? = null

    fun startMonitoring() {
        // Thermal monitoring requires Android Q (API 29) and is temporarily disabled
        // due to API compatibility issues. Can be re-enabled when proper API support is available.
        Log.d(TAG, "Thermal monitoring not available on this device")
    }

    fun stopMonitoring() {
        scope.cancel()
    }

    private fun checkThermalAndUpdateDownloads(status: Int) {
        scope.launch {
            val overheatingProtection = dataStoreManager.overheatingProtection.first() ?: true

            if (!overheatingProtection) {
                Log.d(TAG, "Overheating protection disabled")
                return@launch
            }

            // Pause at MODERATE or higher
            val shouldPause = status >= PowerManager.THERMAL_STATUS_MODERATE
            _shouldPauseDownloads.value = shouldPause

            if (shouldPause) {
                val statusName = getThermalStatusName(status)
                Log.d(TAG, "Device overheating ($statusName). Pausing downloads.")
                pauseActiveDownloads()
            } else {
                Log.d(TAG, "Thermal status OK. Resuming downloads if needed.")
                resumePausedDownloads()
            }
        }
    }

    private fun getThermalStatusName(status: Int): String {
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "NONE"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "UNKNOWN"
        }
    }

    private suspend fun pauseActiveDownloads() {
        val downloadingPlaylists = database.downloadedPlaylistDao()
            .getPlaylistsByStatus(PlaylistDownloadStatus.DOWNLOADING)

        downloadingPlaylists.collect { playlists ->
            playlists.forEach { playlist ->
                database.downloadedPlaylistDao().updatePlaylistStatus(
                    playlist.playlistId,
                    PlaylistDownloadStatus.PAUSED
                )
                workManager.pausePlaylistDownload(playlist.playlistId)
            }
        }
    }

    private suspend fun resumePausedDownloads() {
        val pausedPlaylists = database.downloadedPlaylistDao()
            .getPlaylistsByStatus(PlaylistDownloadStatus.PAUSED)

        pausedPlaylists.collect { playlists ->
            playlists.forEach { playlist ->
                database.downloadedPlaylistDao().updatePlaylistStatus(
                    playlist.playlistId,
                    PlaylistDownloadStatus.DOWNLOADING
                )
                resumePlaylistDownload(playlist.playlistId, playlist.name)
            }
        }
    }

    private suspend fun resumePlaylistDownload(playlistId: String, playlistName: String) {
        val crossRefs = database.playlistSongCrossRefDao()
            .getSongsByStatus(playlistId, pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus.PENDING) +
                database.playlistSongCrossRefDao()
                    .getSongsByStatus(playlistId, pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus.FAILED)

        if (crossRefs.isEmpty()) return

        val songIds = crossRefs.map { it.songId }
        val songTitles = crossRefs.map { "Song" }
        val fileSizes = crossRefs.map { it.fileSize }
        val quality = dataStoreManager.downloadQuality.first().name ?: "ORIGINAL"
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
