package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.cache.CacheManager
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadQueueEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality

class DownloadManager private constructor(
    private val context: Context,
    private val database: OfflineDatabase,
    private val fileDownloader: FileDownloader,
    private val dataStoreManager: DataStoreManager,
    private val cacheManager: CacheManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "DownloadManager"

        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(
            context: Context,
            database: OfflineDatabase,
            fileDownloader: FileDownloader,
            dataStoreManager: DataStoreManager,
            cacheManager: CacheManager
        ): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(
                    context.applicationContext,
                    database,
                    fileDownloader,
                    dataStoreManager,
                    cacheManager
                ).also { instance = it }
            }
        }
    }

    suspend fun enqueueDownload(song: Song, quality: StreamingQuality) {
        try {
            // Check if already downloaded
            val existing = database.downloadedSongDao().getSongById(song.id)
            if (existing != null) {
                Log.d(TAG, "Song ${song.id} already downloaded")
                return
            }

            // Check if already in queue
            val inQueue = database.downloadQueueDao().getBySongId(song.id)
            if (inQueue != null) {
                Log.d(TAG, "Song ${song.id} already in queue")
                return
            }

            // Add to queue
            val queueItem = DownloadQueueEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                quality = quality.name,
                status = DownloadStatus.PENDING,
                progress = 0f,
                bytesDownloaded = 0,
                totalBytes = 0,
                addedDate = System.currentTimeMillis(),
                errorMessage = null,
                retryCount = 0
            )

            database.downloadQueueDao().insert(queueItem)
            Log.d(TAG, "Enqueued download for: ${song.title}")

            // Trigger download processing
            processQueue()
        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing download: ${e.message}", e)
        }
    }

    private fun processQueue() {
        scope.launch {
            try {
                val nextItem = database.downloadQueueDao().getNextPendingDownload()
                if (nextItem != null) {
                    downloadItem(nextItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queue: ${e.message}", e)
            }
        }
    }

    private suspend fun downloadItem(queueItem: DownloadQueueEntity) {
        try {
            Log.d(TAG, "Starting download: ${queueItem.title}")

            // Update status to downloading
            database.downloadQueueDao().update(
                queueItem.copy(status = DownloadStatus.DOWNLOADING)
            )

            val quality = StreamingQuality.valueOf(queueItem.quality)

            // Download audio file
            val downloadResult = fileDownloader.downloadSong(
                songId = queueItem.songId,
                quality = quality
            ) { progress, bytesDownloaded, totalBytes ->
                // Update progress
                scope.launch {
                    database.downloadQueueDao().updateProgress(
                        queueItem.queueId,
                        progress,
                        bytesDownloaded
                    )
                }
            }

            when (downloadResult) {
                is DownloadResult.Success -> {
                    Log.d(TAG, "Download successful: ${queueItem.title}")

                    // Download album art (optional, don't fail if this fails)
                    val imagePath = try {
                        fileDownloader.downloadImage(
                            itemId = queueItem.songId,
                            imageType = "Primary",
                            tag = "default"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to download image: ${e.message}")
                        null
                    }

                    // Save to downloaded songs database
                    val downloadedSong = DownloadedSongEntity(
                        id = queueItem.songId,
                        title = queueItem.title,
                        artist = queueItem.artist,
                        album = null,
                        duration = "0:00", // TODO: Get actual duration
                        quality = queueItem.quality,
                        filePath = downloadResult.filePath,
                        imagePath = imagePath,
                        downloadDate = System.currentTimeMillis(),
                        fileSize = downloadResult.size,
                        imageSize = imagePath?.let { java.io.File(it).length() } ?: 0,
                        lastAccessTime = System.currentTimeMillis(),
                        bitrate = quality.maxBitrate,
                        format = downloadResult.format,
                        albumId = null,
                        artistId = null
                    )

                    database.downloadedSongDao().insert(downloadedSong)

                    // Remove from queue
                    database.downloadQueueDao().update(
                        queueItem.copy(status = DownloadStatus.COMPLETED)
                    )
                    database.downloadQueueDao().delete(queueItem)

                    // Check cache limits and evict if needed
                    cacheManager.evictIfNeeded()

                    Log.d(TAG, "Download completed successfully: ${queueItem.title}")
                }

                is DownloadResult.Failure -> {
                    Log.e(TAG, "Download failed: ${downloadResult.error}")

                    // Update status to failed
                    database.downloadQueueDao().update(
                        queueItem.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = downloadResult.error,
                            retryCount = queueItem.retryCount + 1
                        )
                    )
                }
            }

            // Process next item in queue
            processQueue()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading item: ${e.message}", e)

            // Mark as failed
            database.downloadQueueDao().update(
                queueItem.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message,
                    retryCount = queueItem.retryCount + 1
                )
            )
        }
    }

    fun getDownloadStatus(songId: String): Flow<DownloadStatus?> {
        return database.downloadQueueDao().getAllDownloads().map { queue ->
            queue.find { it.songId == songId }?.status
        }
    }

    suspend fun cancelDownload(songId: String) {
        try {
            val queueItem = database.downloadQueueDao().getBySongId(songId)
            if (queueItem != null) {
                database.downloadQueueDao().update(
                    queueItem.copy(status = DownloadStatus.CANCELLED)
                )
                database.downloadQueueDao().delete(queueItem)
                Log.d(TAG, "Cancelled download: $songId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling download: ${e.message}", e)
        }
    }

    suspend fun retryFailedDownload(queueId: Long) {
        try {
            val queueItem = database.downloadQueueDao().getAllDownloads().first()
                .find { it.queueId == queueId }

            if (queueItem != null && queueItem.status == DownloadStatus.FAILED) {
                database.downloadQueueDao().update(
                    queueItem.copy(
                        status = DownloadStatus.PENDING,
                        progress = 0f,
                        bytesDownloaded = 0,
                        errorMessage = null
                    )
                )
                processQueue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying download: ${e.message}", e)
        }
    }

    fun getPendingDownloads(): Flow<List<DownloadQueueEntity>> {
        return database.downloadQueueDao().getByStatus(DownloadStatus.PENDING)
    }

    fun getActiveDownloads(): Flow<List<DownloadQueueEntity>> {
        return database.downloadQueueDao().getByStatus(DownloadStatus.DOWNLOADING)
    }

    fun getFailedDownloads(): Flow<List<DownloadQueueEntity>> {
        return database.downloadQueueDao().getByStatus(DownloadStatus.FAILED)
    }
}
