package pe.net.libre.mixtapehaven.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_songs",
    indices = [
        Index(value = ["lastAccessTime"]),
        Index(value = ["downloadDate"]),
        Index(value = ["albumId"])
    ]
)
data class DownloadedSongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: String,
    val quality: String,
    val filePath: String,
    val imagePath: String?,
    val downloadDate: Long,
    val fileSize: Long,
    val imageSize: Long,
    val lastAccessTime: Long,
    val bitrate: Int?,
    val format: String,
    val albumId: String?,
    val artistId: String?
)
