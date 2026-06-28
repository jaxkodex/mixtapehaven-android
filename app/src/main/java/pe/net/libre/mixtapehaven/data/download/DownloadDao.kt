package pe.net.libre.mixtapehaven.data.download

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Room access to the offline download library. */
@Dao
interface DownloadDao {

    /** All saved/in-progress downloads, oldest first, observed for the UI. */
    @Query("SELECT * FROM downloaded_tracks ORDER BY rowid")
    fun observeAll(): Flow<List<DownloadedTrack>>

    /** Running total of bytes used by saved downloads. */
    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM downloaded_tracks")
    fun observeTotalSize(): Flow<Long>

    @Upsert
    suspend fun upsert(track: DownloadedTrack)

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id")
    suspend fun findById(id: String): DownloadedTrack?

    @Query("SELECT * FROM downloaded_tracks")
    suspend fun getAll(): List<DownloadedTrack>

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun clear()
}
