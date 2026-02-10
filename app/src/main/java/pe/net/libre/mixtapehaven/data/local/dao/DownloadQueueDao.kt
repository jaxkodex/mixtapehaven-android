package pe.net.libre.mixtapehaven.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pe.net.libre.mixtapehaven.data.local.entity.DownloadQueueEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadStatus

@Dao
interface DownloadQueueDao {
    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY addedDate ASC")
    fun getByStatus(status: DownloadStatus): Flow<List<DownloadQueueEntity>>

    @Query("SELECT * FROM download_queue WHERE songId = :songId LIMIT 1")
    suspend fun getBySongId(songId: String): DownloadQueueEntity?

    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY addedDate ASC LIMIT 1")
    suspend fun getNextPendingDownload(status: DownloadStatus = DownloadStatus.PENDING): DownloadQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadQueueEntity): Long

    @Update
    suspend fun update(item: DownloadQueueEntity)

    @Delete
    suspend fun delete(item: DownloadQueueEntity)

    @Query("UPDATE download_queue SET status = :status WHERE queueId = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE download_queue SET progress = :progress, bytesDownloaded = :bytesDownloaded WHERE queueId = :id")
    suspend fun updateProgress(id: Long, progress: Float, bytesDownloaded: Long)

    @Query("DELETE FROM download_queue WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("UPDATE download_queue SET status = :newStatus, progress = 0, bytesDownloaded = 0 WHERE status = :oldStatus")
    suspend fun resetStatus(oldStatus: DownloadStatus, newStatus: DownloadStatus): Int

    @Query("SELECT * FROM download_queue ORDER BY addedDate DESC")
    fun getAllDownloads(): Flow<List<DownloadQueueEntity>>
}
