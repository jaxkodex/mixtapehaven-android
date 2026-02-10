package pe.net.libre.mixtapehaven.data.download.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import java.util.concurrent.TimeUnit

/**
 * Manages WorkManager operations for playlist downloads
 */
class DownloadWorkManager(private val context: Context) {

    companion object {
        const val WORK_TAG_PLAYLIST_DOWNLOAD = "playlist_download"
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_PLAYLIST_NAME = "playlist_name"
        const val KEY_QUALITY = "quality"
        const val KEY_SONG_IDS = "song_ids"
        const val KEY_SONG_TITLES = "song_titles"
        const val KEY_FILE_SIZES = "file_sizes"
        const val KEY_TOTAL_SIZE = "total_size"
        const val KEY_VALIDATION_PASSED = "validation_passed"
        const val KEY_COVER_URL = "cover_url"

        fun getUniqueWorkName(playlistId: String) = "playlist_download_$playlistId"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Start a playlist download
     */
    fun startPlaylistDownload(
        playlistId: String,
        playlistName: String,
        songIds: List<String>,
        songTitles: List<String>,
        fileSizes: List<Long>,
        totalSize: Long,
        quality: String,
        coverUrl: String?
    ) {
        // Create validation work
        val validationWork = createValidationWork(
            playlistId, playlistName, songIds, songTitles, fileSizes, totalSize, quality, coverUrl
        )

        // Enqueue unique work - validation worker will chain to download workers
        workManager.enqueueUniqueWork(
            getUniqueWorkName(playlistId),
            ExistingWorkPolicy.REPLACE,
            validationWork
        )
    }

    /**
     * Pause a playlist download
     */
    fun pausePlaylistDownload(playlistId: String) {
        workManager.cancelUniqueWork(getUniqueWorkName(playlistId))
    }

    /**
     * Resume a playlist download
     */
    fun resumePlaylistDownload(
        playlistId: String,
        playlistName: String,
        remainingSongIds: List<String>,
        remainingSongTitles: List<String>,
        remainingFileSizes: List<Long>,
        quality: String,
        coverUrl: String?
    ) {
        // Skip validation on resume, start download directly
        val downloadWorks = createDownloadWorks(
            playlistId = playlistId,
            playlistName = playlistName,
            songIds = remainingSongIds,
            songTitles = remainingSongTitles,
            fileSizes = remainingFileSizes,
            quality = quality,
            coverUrl = coverUrl,
            skipValidation = true
        )

        workManager.enqueueUniqueWork(
            getUniqueWorkName(playlistId),
            ExistingWorkPolicy.REPLACE,
            downloadWorks
        )
    }

    /**
     * Cancel a playlist download completely
     */
    fun cancelPlaylistDownload(playlistId: String) {
        workManager.cancelUniqueWork(getUniqueWorkName(playlistId))
    }

    /**
     * Check if a playlist download is currently running
     */
    fun isDownloadRunning(playlistId: String): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(getUniqueWorkName(playlistId)).get()
        return workInfos.any { !it.state.isFinished }
    }

    private fun createValidationWork(
        playlistId: String,
        playlistName: String,
        songIds: List<String>,
        songTitles: List<String>,
        fileSizes: List<Long>,
        totalSize: Long,
        quality: String,
        coverUrl: String?
    ): OneTimeWorkRequest {
        val inputData = workDataOf(
            KEY_PLAYLIST_ID to playlistId,
            KEY_PLAYLIST_NAME to playlistName,
            KEY_SONG_IDS to songIds.toTypedArray(),
            KEY_SONG_TITLES to songTitles.toTypedArray(),
            KEY_FILE_SIZES to fileSizes.toTypedArray(),
            KEY_TOTAL_SIZE to totalSize,
            KEY_QUALITY to quality,
            KEY_COVER_URL to coverUrl
        )

        return OneTimeWorkRequestBuilder<PlaylistValidationWorker>()
            .setInputData(inputData)
            .addTag(WORK_TAG_PLAYLIST_DOWNLOAD)
            .addTag("playlist_$playlistId")
            .build()
    }

    fun createDownloadWorks(
        playlistId: String,
        playlistName: String,
        songIds: List<String>,
        songTitles: List<String>,
        fileSizes: List<Long>,
        quality: String,
        coverUrl: String?,
        skipValidation: Boolean = false
    ): OneTimeWorkRequest {
        // Base constraints - WiFi only handled separately via dialog
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            KEY_PLAYLIST_ID to playlistId,
            KEY_PLAYLIST_NAME to playlistName,
            KEY_SONG_IDS to songIds.toTypedArray(),
            KEY_SONG_TITLES to songTitles.toTypedArray(),
            KEY_FILE_SIZES to fileSizes.toTypedArray(),
            KEY_QUALITY to quality,
            KEY_COVER_URL to coverUrl,
            KEY_VALIDATION_PASSED to skipValidation
        )

        return OneTimeWorkRequestBuilder<PlaylistDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_TAG_PLAYLIST_DOWNLOAD)
            .addTag("playlist_$playlistId")
            .build()
    }

    /**
     * Create completion worker to run after all downloads finish
     */
    fun createCompletionWork(playlistId: String): OneTimeWorkRequest {
        val inputData = workDataOf(KEY_PLAYLIST_ID to playlistId)

        return OneTimeWorkRequestBuilder<PlaylistCompletionWorker>()
            .setInputData(inputData)
            .addTag(WORK_TAG_PLAYLIST_DOWNLOAD)
            .addTag("playlist_$playlistId")
            .build()
    }
}
