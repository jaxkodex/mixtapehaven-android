package pe.net.libre.mixtapehaven.data.download

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Room access to the offline video download library. */
@Dao
interface VideoDownloadDao {

    /** All saved/in-progress video downloads, oldest first, observed for the UI. */
    @Query("SELECT * FROM downloaded_videos ORDER BY rowid")
    fun observeAll(): Flow<List<DownloadedVideo>>

    /** Running total of bytes used by completed video downloads. */
    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM downloaded_videos WHERE complete = 1")
    fun observeTotalSize(): Flow<Long>

    @Upsert
    suspend fun upsert(video: DownloadedVideo)

    @Query("SELECT * FROM downloaded_videos WHERE id = :id")
    suspend fun findById(id: String): DownloadedVideo?

    /** Move [id] to [status] without touching the rest of the row; no-op if the row is gone. */
    @Query("UPDATE downloaded_videos SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Ids of rows not yet complete, for reconciling against WorkManager's queue at startup. */
    @Query("SELECT id FROM downloaded_videos WHERE complete = 0")
    suspend fun incompleteIds(): List<String>

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloaded_videos")
    suspend fun clear()
}
