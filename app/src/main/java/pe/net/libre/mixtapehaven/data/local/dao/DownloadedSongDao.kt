package pe.net.libre.mixtapehaven.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity

@Dao
interface DownloadedSongDao {
    @Query("SELECT * FROM downloaded_songs WHERE id = :songId")
    suspend fun getSongById(songId: String): DownloadedSongEntity?

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadDate DESC")
    fun getAllDownloaded(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT SUM(fileSize + imageSize) FROM downloaded_songs")
    suspend fun getTotalCacheSize(): Long?

    @Query("SELECT * FROM downloaded_songs ORDER BY lastAccessTime ASC LIMIT :count")
    suspend fun getLeastRecentlyUsed(count: Int): List<DownloadedSongEntity>

    @Query("UPDATE downloaded_songs SET lastAccessTime = :timestamp WHERE id = :songId")
    suspend fun updateLastAccessTime(songId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: DownloadedSongEntity)

    @Delete
    suspend fun delete(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM downloaded_songs")
    suspend fun getCount(): Int

    @Query("SELECT * FROM downloaded_songs WHERE albumId = :albumId")
    suspend fun getSongsByAlbumId(albumId: String): List<DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    suspend fun searchSongs(query: String): List<DownloadedSongEntity>
}
