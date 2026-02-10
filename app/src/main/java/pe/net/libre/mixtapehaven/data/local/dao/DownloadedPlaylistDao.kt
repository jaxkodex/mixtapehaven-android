package pe.net.libre.mixtapehaven.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedPlaylistEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadPreferenceEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistSongCrossRef
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus

@Dao
interface DownloadedPlaylistDao {
    @Query("SELECT * FROM downloaded_playlists ORDER BY downloadDate DESC")
    fun getAllDownloadedPlaylists(): Flow<List<DownloadedPlaylistEntity>>

    @Query("SELECT * FROM downloaded_playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: String): DownloadedPlaylistEntity?

    @Query("SELECT * FROM downloaded_playlists WHERE status = :status")
    fun getPlaylistsByStatus(status: PlaylistDownloadStatus): Flow<List<DownloadedPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: DownloadedPlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: DownloadedPlaylistEntity)

    @Query("DELETE FROM downloaded_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_playlists WHERE playlistId = :playlistId AND status = :status)")
    suspend fun isPlaylistInStatus(playlistId: String, status: PlaylistDownloadStatus): Boolean

    @Query("UPDATE downloaded_playlists SET status = :status, lastUpdated = :timestamp WHERE playlistId = :playlistId")
    suspend fun updatePlaylistStatus(playlistId: String, status: PlaylistDownloadStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE downloaded_playlists SET downloadedSongs = :downloadedCount, failedSongs = :failedCount, lastUpdated = :timestamp WHERE playlistId = :playlistId")
    suspend fun updatePlaylistProgress(playlistId: String, downloadedCount: Int, failedCount: Int, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface PlaylistSongCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getSongsForPlaylist(playlistId: String): List<PlaylistSongCrossRef>

    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND downloadStatus = :status")
    suspend fun getSongsByStatus(playlistId: String, status: SongDownloadStatus): List<PlaylistSongCrossRef>

    @Query("UPDATE playlist_song_cross_ref SET downloadStatus = :status WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongStatus(playlistId: String, songId: String, status: SongDownloadStatus)

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND downloadStatus = :status")
    suspend fun countSongsByStatus(playlistId: String, status: SongDownloadStatus): Int

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteSongsForPlaylist(playlistId: String)

    @Query("SELECT fileSize FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun getSongFileSize(playlistId: String, songId: String): Long?
}

@Dao
interface PlaylistDownloadPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PlaylistDownloadPreferenceEntity)

    @Query("SELECT * FROM playlist_download_preferences WHERE playlistId = :playlistId")
    suspend fun getPreference(playlistId: String): PlaylistDownloadPreferenceEntity?

    @Query("DELETE FROM playlist_download_preferences WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredPreferences(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlist_download_preferences WHERE playlistId = :playlistId")
    suspend fun deletePreference(playlistId: String)
}
