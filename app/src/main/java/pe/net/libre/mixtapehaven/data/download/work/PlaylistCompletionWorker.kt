package pe.net.libre.mixtapehaven.data.download.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus

/**
 * Handles completion of playlist download
 * Updates final status and cleans up
 */
class PlaylistCompletionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = OfflineDatabase.getInstance(context)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val playlistId = inputData.getString(DownloadWorkManager.KEY_PLAYLIST_ID)
                    ?: return@withContext Result.failure()

                // Count completed and failed songs
                val crossRefs = database.playlistSongCrossRefDao().getSongsForPlaylist(playlistId)
                val completedCount = crossRefs.count { it.downloadStatus == SongDownloadStatus.COMPLETED }
                val failedCount = crossRefs.count { it.downloadStatus == SongDownloadStatus.FAILED }
                val totalCount = crossRefs.size

                // Determine final status
                val finalStatus = when {
                    failedCount == 0 -> PlaylistDownloadStatus.FULL
                    completedCount > 0 -> PlaylistDownloadStatus.PARTIAL
                    else -> PlaylistDownloadStatus.CANCELLED
                }

                // Update playlist status
                database.downloadedPlaylistDao().updatePlaylistStatus(playlistId, finalStatus)
                database.downloadedPlaylistDao().updatePlaylistProgress(
                    playlistId,
                    completedCount,
                    failedCount
                )

                // Clean up download queue entries for this playlist
                database.downloadQueueDao().deleteByPlaylistId(playlistId)

                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}
