package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room database holding the offline download library (audio tracks and videos). */
@Database(
    entities = [DownloadedTrack::class, DownloadedVideo::class, VideoProgress::class],
    version = 3,
    exportSchema = true,
)
abstract class DownloadDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    abstract fun videoDownloadDao(): VideoDownloadDao

    abstract fun videoProgressDao(): VideoProgressDao

    companion object {
        /** v1 -> v2: adds the video download table. Additive so existing audio downloads survive. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `downloaded_videos` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                        "`seriesName` TEXT, `seasonEpisodeLabel` TEXT, `runtimeLabel` TEXT NOT NULL, " +
                        "`posterUrl` TEXT, `artColorArgb` INTEGER NOT NULL, `qualityLabel` TEXT NOT NULL, " +
                        "`filePath` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `complete` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
            }
        }

        /** v2 -> v3: adds the local watch-position table backing offline resume and Continue watching. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `video_progress` (" +
                        "`id` TEXT NOT NULL, `positionMs` INTEGER NOT NULL, `runtimeMs` INTEGER NOT NULL, " +
                        "`updatedAtMs` INTEGER NOT NULL, `title` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                        "`seriesName` TEXT, `seriesId` TEXT, `seasonEpisodeLabel` TEXT, `posterUrl` TEXT, " +
                        "`backdropUrl` TEXT, `artColorArgb` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        /** Build the app's download database in the default location. */
        fun build(context: Context): DownloadDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DownloadDatabase::class.java,
                "downloads.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
