package pe.net.libre.mixtapehaven.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(
    tableName = "download_queue",
    indices = [
        Index(value = ["status", "addedDate"]),
        Index(value = ["songId"]),
        Index(value = ["playlistId"])
    ]
)
data class DownloadQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val queueId: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val quality: String,
    val status: DownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val addedDate: Long,
    val errorMessage: String?,
    val retryCount: Int,
    val playlistId: String? = null,
    val playlistSongIndex: Int = 0
)
