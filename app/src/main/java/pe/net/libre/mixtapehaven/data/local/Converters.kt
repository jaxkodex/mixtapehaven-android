package pe.net.libre.mixtapehaven.data.local

import androidx.room.TypeConverter
import pe.net.libre.mixtapehaven.data.local.entity.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return DownloadStatus.valueOf(value)
    }
}
