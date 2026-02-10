package pe.net.libre.mixtapehaven.data.download.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pe.net.libre.mixtapehaven.data.download.FileDownloader
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadQueueEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import java.io.File

/**
 * Downloads songs from a playlist using parallel workers
 */
class PlaylistDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PlaylistDownloadWorker"
        const val PROGRESS_CURRENT_SONG = "progress_current_song"
        const val PROGRESS_TOTAL_SONGS = "progress_total_songs"
        const val PROGRESS_PERCENT = "progress_percent"
        const val KEY_CURRENT_SONG_INDEX = "current_song_index"
    }

    private val database = OfflineDatabase.getInstance(context)
    private val dataStoreManager = DataStoreManager(context)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val playlistId = inputData.getString(DownloadWorkManager.KEY_PLAYLIST_ID)
                    ?: return@withContext Result.failure()

                val playlistName = inputData.getString(DownloadWorkManager.KEY_PLAYLIST_NAME)
                    ?: "Unknown"

                val songIds = inputData.getStringArray(DownloadWorkManager.KEY_SONG_IDS)?.toList()
                    ?: return@withContext Result.failure()

                val songTitles = inputData.getStringArray(DownloadWorkManager.KEY_SONG_TITLES)?.toList()
                    ?: emptyList()

                val fileSizes = inputData.getLongArray(DownloadWorkManager.KEY_FILE_SIZES)?.toList()
                    ?: emptyList()

                val quality = inputData.getString(DownloadWorkManager.KEY_QUALITY) ?: "ORIGINAL"

                // Get songs that are still pending or failed
                val songsToDownload = getPendingSongs(playlistId, songIds, songTitles, fileSizes)

                if (songsToDownload.isEmpty()) {
                    // All songs already downloaded or nothing to do
                    return@withContext Result.success()
                }

                // Update playlist status to DOWNLOADING
                database.downloadedPlaylistDao().updatePlaylistStatus(
                    playlistId,
                    PlaylistDownloadStatus.DOWNLOADING
                )

                // Download each song sequentially with progress updates
                val totalSongs = songIds.size
                var completedSongs = 0
                var failedSongs = 0

                songsToDownload.forEachIndexed { index, songInfo ->
                    // Check if work was cancelled
                    if (isStopped) {
                        return@withContext Result.retry()
                    }

                    val currentProgress = ((completedSongs + failedSongs).toFloat() / totalSongs * 100).toInt()
                    setProgress(
                        workDataOf(
                            PROGRESS_CURRENT_SONG to completedSongs + failedSongs + 1,
                            PROGRESS_TOTAL_SONGS to totalSongs,
                            PROGRESS_PERCENT to currentProgress,
                            DownloadWorkManager.KEY_PLAYLIST_ID to playlistId,
                            DownloadWorkManager.KEY_PLAYLIST_NAME to playlistName
                        )
                    )

                    // Update song status to DOWNLOADING
                    database.playlistSongCrossRefDao().updateSongStatus(
                        playlistId,
                        songInfo.id,
                        SongDownloadStatus.DOWNLOADING
                    )

                    // Add to download queue
                    val queueItem = DownloadQueueEntity(
                        songId = songInfo.id,
                        title = songInfo.title,
                        artist = "", // Will be updated during download
                        quality = quality,
                        status = DownloadStatus.PENDING,
                        progress = 0f,
                        bytesDownloaded = 0,
                        totalBytes = songInfo.fileSize,
                        addedDate = System.currentTimeMillis(),
                        errorMessage = null,
                        retryCount = 0,
                        playlistId = playlistId,
                        playlistSongIndex = index
                    )
                    database.downloadQueueDao().insert(queueItem)

                    // Perform download
                    val success = downloadSong(songInfo.id, quality)

                    if (success) {
                        database.playlistSongCrossRefDao().updateSongStatus(
                            playlistId,
                            songInfo.id,
                            SongDownloadStatus.COMPLETED
                        )
                        completedSongs++
                    } else {
                        database.playlistSongCrossRefDao().updateSongStatus(
                            playlistId,
                            songInfo.id,
                            SongDownloadStatus.FAILED
                        )
                        failedSongs++
                    }

                    // Update playlist progress
                    database.downloadedPlaylistDao().updatePlaylistProgress(
                        playlistId,
                        completedSongs,
                        failedSongs
                    )

                    // Small delay to prevent overwhelming the system
                    delay(100)
                }

                // Final progress update
                setProgress(
                    workDataOf(
                        PROGRESS_CURRENT_SONG to completedSongs + failedSongs,
                        PROGRESS_TOTAL_SONGS to totalSongs,
                        PROGRESS_PERCENT to 100,
                        DownloadWorkManager.KEY_PLAYLIST_ID to playlistId,
                        DownloadWorkManager.KEY_PLAYLIST_NAME to playlistName
                    )
                )

                Result.success(
                    workDataOf(
                        DownloadWorkManager.KEY_PLAYLIST_ID to playlistId,
                        "completed_songs" to completedSongs as Int,
                        "failed_songs" to failedSongs as Int
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                Result.retry()
            }
        }
    }

    private suspend fun getPendingSongs(
        playlistId: String,
        songIds: List<String>,
        songTitles: List<String>,
        fileSizes: List<Long>
    ): List<SongInfo> {
        val crossRefs = database.playlistSongCrossRefDao().getSongsForPlaylist(playlistId)

        return songIds.mapIndexedNotNull { index, songId ->
            val crossRef = crossRefs.find { it.songId == songId }
            val status = crossRef?.downloadStatus ?: SongDownloadStatus.PENDING

            // Only download if pending or failed
            if (status == SongDownloadStatus.PENDING || status == SongDownloadStatus.FAILED) {
                SongInfo(
                    id = songId,
                    title = songTitles.getOrNull(index) ?: "Unknown",
                    fileSize = fileSizes.getOrNull(index) ?: 0L
                )
            } else {
                null
            }
        }
    }

    private suspend fun downloadSong(songId: String, quality: String): Boolean {
        return try {
            // Get server info from DataStore
            val serverUrl = dataStoreManager.serverUrl.first()
            val accessToken = dataStoreManager.accessToken.first()
            val userId = dataStoreManager.userId.first()

            if (serverUrl == null || accessToken == null || userId == null) {
                Log.e(TAG, "Missing server credentials")
                return false
            }

            // Create download directory
            val musicDir = File(applicationContext.getExternalFilesDir("music"), "downloads")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val outputFile = File(musicDir, "$songId.mp3")

            // Download using FileDownloader
            val downloader = FileDownloader()
            val success = downloader.downloadSong(
                serverUrl = serverUrl,
                accessToken = accessToken,
                userId = userId,
                itemId = songId,
                quality = quality,
                outputFile = outputFile,
                onProgress = { progress, bytesDownloaded, totalBytes ->
                    // Update progress in database
                    // This runs on a background thread
                }
            )

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading song $songId", e)
            false
        }
    }

    private data class SongInfo(
        val id: String,
        val title: String,
        val fileSize: Long
    )
}
