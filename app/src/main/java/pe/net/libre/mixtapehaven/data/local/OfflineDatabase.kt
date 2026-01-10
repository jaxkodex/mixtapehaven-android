package pe.net.libre.mixtapehaven.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pe.net.libre.mixtapehaven.data.local.dao.DownloadQueueDao
import pe.net.libre.mixtapehaven.data.local.dao.DownloadedSongDao
import pe.net.libre.mixtapehaven.data.local.entity.DownloadQueueEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity

@Database(
    entities = [
        DownloadedSongEntity::class,
        DownloadQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun downloadQueueDao(): DownloadQueueDao

    companion object {
        @Volatile
        private var instance: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_database"
                ).build().also { instance = it }
            }
        }
    }
}
