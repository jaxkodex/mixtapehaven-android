package pe.net.libre.mixtapehaven.data.repository

import kotlinx.coroutines.flow.Flow
import pe.net.libre.mixtapehaven.data.cache.CacheManager
import pe.net.libre.mixtapehaven.data.cache.CacheStatistics
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality

class OfflineRepository(
    private val database: OfflineDatabase,
    private val downloadManager: DownloadManager,
    private val cacheManager: CacheManager
) {
    suspend fun isDownloaded(songId: String): Boolean {
        return database.downloadedSongDao().getSongById(songId) != null
    }

    suspend fun getDownloadedSong(songId: String): DownloadedSongEntity? {
        return database.downloadedSongDao().getSongById(songId)
    }

    fun getAllDownloaded(): Flow<List<DownloadedSongEntity>> {
        return database.downloadedSongDao().getAllDownloaded()
    }

    suspend fun downloadSong(song: Song, quality: StreamingQuality) {
        downloadManager.enqueueDownload(song, quality)
    }

    suspend fun downloadPlaylist(songs: List<Song>, quality: StreamingQuality) {
        downloadManager.enqueuePlaylistDownload(songs, quality)
    }

    suspend fun deleteSong(songId: String) {
        val song = database.downloadedSongDao().getSongById(songId)
        song?.let { cacheManager.deleteSong(it) }
    }

    suspend fun getCacheStatistics(): CacheStatistics {
        return cacheManager.getCacheStatistics()
    }

    suspend fun clearCache() {
        cacheManager.clearCache()
    }

    suspend fun updateLastAccessTime(songId: String) {
        database.downloadedSongDao().updateLastAccessTime(songId, System.currentTimeMillis())
    }

    suspend fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
    }

    fun getActiveDownloads() = downloadManager.getActiveDownloads()

    fun getPendingDownloads() = downloadManager.getPendingDownloads()

    fun getFailedDownloads() = downloadManager.getFailedDownloads()

    suspend fun searchDownloadedSongs(query: String): List<DownloadedSongEntity> {
        return database.downloadedSongDao().searchSongs(query)
    }
}
