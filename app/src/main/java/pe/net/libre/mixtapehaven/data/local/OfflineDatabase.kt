package pe.net.libre.mixtapehaven.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pe.net.libre.mixtapehaven.data.local.dao.DownloadQueueDao
import pe.net.libre.mixtapehaven.data.local.dao.DownloadedPlaylistDao
import pe.net.libre.mixtapehaven.data.local.dao.DownloadedSongDao
import pe.net.libre.mixtapehaven.data.local.dao.PlaylistDownloadPreferenceDao
import pe.net.libre.mixtapehaven.data.local.dao.PlaylistSongCrossRefDao
import pe.net.libre.mixtapehaven.data.local.entity.DownloadQueueEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedPlaylistEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadPreferenceEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        DownloadedSongEntity::class,
        DownloadQueueEntity::class,
        DownloadedPlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlaylistDownloadPreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun downloadQueueDao(): DownloadQueueDao
    abstract fun downloadedPlaylistDao(): DownloadedPlaylistDao
    abstract fun playlistSongCrossRefDao(): PlaylistSongCrossRefDao
    abstract fun playlistDownloadPreferenceDao(): PlaylistDownloadPreferenceDao

    companion object {
        @Volatile
        private var instance: OfflineDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add playlist columns to download_queue
                database.execSQL("ALTER TABLE download_queue ADD COLUMN playlistId TEXT")
                database.execSQL("ALTER TABLE download_queue ADD COLUMN playlistSongIndex INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_download_queue_playlistId ON download_queue(playlistId)")

                // Create downloaded_playlists table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS downloaded_playlists (
                        playlistId TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        totalSongs INTEGER NOT NULL,
                        downloadedSongs INTEGER NOT NULL,
                        failedSongs INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        coverUrl TEXT,
                        lastUpdated INTEGER NOT NULL,
                        downloadDate INTEGER NOT NULL
                    )
                """)

                // Create playlist_song_cross_ref table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref (
                        playlistId TEXT NOT NULL,
                        songId TEXT NOT NULL,
                        downloadStatus TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, songId)
                    )
                """)

                // Create playlist_download_preferences table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_download_preferences (
                        playlistId TEXT PRIMARY KEY NOT NULL,
                        allowMobileData INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): OfflineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
