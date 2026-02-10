package pe.net.libre.mixtapehaven.data.download.work

import android.content.Context
import android.os.StatFs
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_FILE_SIZES
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_PLAYLIST_ID
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_QUALITY
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_SONG_IDS
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_SONG_TITLES
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager.Companion.KEY_TOTAL_SIZE
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedPlaylistEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistSongCrossRef
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus

/**
 * Validates storage space and initializes playlist download
 */
class PlaylistValidationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val BUFFER_SIZE_MB = 500L * 1024 * 1024 // 500MB buffer
        const val OUTPUT_VALIDATION_SUCCESS = "validation_success"
        const val OUTPUT_ERROR_MESSAGE = "error_message"
    }

    private val database = OfflineDatabase.getInstance(context)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val playlistId = inputData.getString(KEY_PLAYLIST_ID)
                    ?: return@withContext Result.failure(
                        workDataOf(OUTPUT_ERROR_MESSAGE to "Missing playlist ID")
                    )

                val playlistName = inputData.getString(DownloadWorkManager.KEY_PLAYLIST_NAME)
                    ?: "Unknown Playlist"

                val songIds = inputData.getStringArray(KEY_SONG_IDS)?.toList() ?: emptyList()
                val songTitles = inputData.getStringArray(KEY_SONG_TITLES)?.toList() ?: emptyList()
                val fileSizes = inputData.getLongArray(KEY_FILE_SIZES)?.toList() ?: emptyList()
                val totalSize = inputData.getLong(KEY_TOTAL_SIZE, 0)
                val quality = inputData.getString(KEY_QUALITY) ?: "ORIGINAL"
                val coverUrl = inputData.getString(DownloadWorkManager.KEY_COVER_URL)

                // Check storage space
                if (!hasEnoughStorage(totalSize)) {
                    val errorMsg = "Insufficient storage space. Need ${formatBytes(totalSize + BUFFER_SIZE_MB)}"
                    return@withContext Result.failure(
                        workDataOf(OUTPUT_ERROR_MESSAGE to errorMsg)
                    )
                }

                // Initialize playlist record in database
                val playlistEntity = DownloadedPlaylistEntity(
                    playlistId = playlistId,
                    name = playlistName,
                    totalSongs = songIds.size,
                    downloadedSongs = 0,
                    failedSongs = 0,
                    status = PlaylistDownloadStatus.DOWNLOADING,
                    coverUrl = coverUrl,
                    lastUpdated = System.currentTimeMillis(),
                    downloadDate = System.currentTimeMillis()
                )
                database.downloadedPlaylistDao().insertPlaylist(playlistEntity)

                // Initialize song cross-references
                val crossRefs = songIds.mapIndexed { index, songId ->
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId = songId,
                        downloadStatus = SongDownloadStatus.PENDING,
                        fileSize = fileSizes.getOrNull(index) ?: 0L
                    )
                }
                database.playlistSongCrossRefDao().insertCrossRefs(crossRefs)

                // Return success - work manager will continue to download worker
                Result.success(
                    workDataOf(
                        OUTPUT_VALIDATION_SUCCESS to true,
                        KEY_PLAYLIST_ID to playlistId,
                        DownloadWorkManager.KEY_PLAYLIST_NAME to playlistName,
                        KEY_SONG_IDS to songIds.toTypedArray(),
                        KEY_SONG_TITLES to songTitles.toTypedArray(),
                        KEY_FILE_SIZES to fileSizes.toTypedArray(),
                        KEY_QUALITY to quality,
                        DownloadWorkManager.KEY_COVER_URL to coverUrl
                    )
                )

            } catch (e: Exception) {
                Result.failure(
                    workDataOf(OUTPUT_ERROR_MESSAGE to (e.message ?: "Validation failed"))
                )
            }
        }
    }

    private fun hasEnoughStorage(requiredBytes: Long): Boolean {
        val stat = StatFs(applicationContext.getExternalFilesDir(null)?.path)
        val available = stat.availableBytes
        return available > requiredBytes + BUFFER_SIZE_MB
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return when {
            mb < 1024 -> "${mb}MB"
            else -> "${mb / 1024}GB"
        }
    }
}
