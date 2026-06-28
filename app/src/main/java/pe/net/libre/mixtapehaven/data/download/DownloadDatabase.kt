package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Room database holding the offline download library. */
@Database(entities = [DownloadedTrack::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        /** Build the app's download database in the default location. */
        fun build(context: Context): DownloadDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DownloadDatabase::class.java,
                "downloads.db",
            ).build()
    }
}
