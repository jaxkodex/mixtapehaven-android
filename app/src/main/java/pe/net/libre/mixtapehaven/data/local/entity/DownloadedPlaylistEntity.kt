package pe.net.libre.mixtapehaven.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlaylistDownloadStatus {
    PENDING,
    DOWNLOADING,
    FULL,
    PARTIAL,
    PAUSED,
    CANCELLED
}

enum class SongDownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloaded_playlists")
data class DownloadedPlaylistEntity(
    @PrimaryKey
    val playlistId: String,
    val name: String,
    val totalSongs: Int,
    val downloadedSongs: Int,
    val failedSongs: Int,
    val status: PlaylistDownloadStatus,
    val coverUrl: String?,
    val lastUpdated: Long,
    val downloadDate: Long
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val downloadStatus: SongDownloadStatus,
    val fileSize: Long
)

@Entity(tableName = "playlist_download_preferences")
data class PlaylistDownloadPreferenceEntity(
    @PrimaryKey
    val playlistId: String,
    val allowMobileData: Boolean,
    val expiresAt: Long
)
