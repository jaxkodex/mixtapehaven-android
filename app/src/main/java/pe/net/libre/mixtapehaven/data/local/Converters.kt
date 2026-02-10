package pe.net.libre.mixtapehaven.data.local

import androidx.room.TypeConverter
import pe.net.libre.mixtapehaven.data.local.entity.DownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return DownloadStatus.valueOf(value)
    }

    @TypeConverter
    fun fromPlaylistDownloadStatus(value: PlaylistDownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toPlaylistDownloadStatus(value: String): PlaylistDownloadStatus {
        return PlaylistDownloadStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSongDownloadStatus(value: SongDownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSongDownloadStatus(value: String): SongDownloadStatus {
        return SongDownloadStatus.valueOf(value)
    }
}
