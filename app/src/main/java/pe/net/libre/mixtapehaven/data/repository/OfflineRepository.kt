package pe.net.libre.mixtapehaven.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pe.net.libre.mixtapehaven.data.cache.CacheManager
import pe.net.libre.mixtapehaven.data.cache.CacheStatistics
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.work.DownloadWorkManager
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedPlaylistEntity
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadPreferenceEntity
import pe.net.libre.mixtapehaven.data.local.entity.PlaylistDownloadStatus
import pe.net.libre.mixtapehaven.data.local.entity.SongDownloadStatus
import pe.net.libre.mixtapehaven.data.model.PlaylistWithSongs
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality

class OfflineRepository(
    private val context: Context,
    private val database: OfflineDatabase,
    private val downloadManager: DownloadManager,
    private val cacheManager: CacheManager
) {
    private val workManager = DownloadWorkManager(context)
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

    // Playlist download methods

    suspend fun downloadPlaylist(playlist: PlaylistWithSongs, quality: String) {
        val songIds = playlist.songs.map { it.id }
        val songTitles = playlist.songs.map { it.title }
        val fileSizes = playlist.songs.map { it.fileSize }

        workManager.startPlaylistDownload(
            playlistId = playlist.playlistId,
            playlistName = playlist.name,
            songIds = songIds,
            songTitles = songTitles,
            fileSizes = fileSizes,
            totalSize = playlist.totalSize,
            quality = quality,
            coverUrl = playlist.coverUrl
        )
    }

    suspend fun pausePlaylistDownload(playlistId: String) {
        workManager.pausePlaylistDownload(playlistId)
        database.downloadedPlaylistDao().updatePlaylistStatus(
            playlistId,
            PlaylistDownloadStatus.PAUSED
        )
    }

    suspend fun resumePlaylistDownload(playlistId: String) {
        val playlist = database.downloadedPlaylistDao().getPlaylistById(playlistId)
            ?: return

        // Get remaining songs to download
        val crossRefs = database.playlistSongCrossRefDao()
            .getSongsByStatus(playlistId, SongDownloadStatus.PENDING) +
                database.playlistSongCrossRefDao()
                    .getSongsByStatus(playlistId, SongDownloadStatus.FAILED)

        if (crossRefs.isEmpty()) {
            // All done, mark as complete
            database.downloadedPlaylistDao().updatePlaylistStatus(
                playlistId,
                PlaylistDownloadStatus.FULL
            )
            return
        }

        val songIds = crossRefs.map { it.songId }
        val songTitles = crossRefs.map { "Song" } // Would need to fetch actual titles
        val fileSizes = crossRefs.map { it.fileSize }

        // Get quality preference
        // Note: Would need to inject DataStoreManager here

        workManager.resumePlaylistDownload(
            playlistId = playlistId,
            playlistName = playlist.name,
            remainingSongIds = songIds,
            remainingSongTitles = songTitles,
            remainingFileSizes = fileSizes,
            quality = "ORIGINAL", // Default, should come from settings
            coverUrl = playlist.coverUrl
        )

        database.downloadedPlaylistDao().updatePlaylistStatus(
            playlistId,
            PlaylistDownloadStatus.DOWNLOADING
        )
    }

    suspend fun cancelPlaylistDownload(playlistId: String) {
        workManager.cancelPlaylistDownload(playlistId)
        database.downloadedPlaylistDao().updatePlaylistStatus(
            playlistId,
            PlaylistDownloadStatus.CANCELLED
        )
        database.downloadQueueDao().deleteByPlaylistId(playlistId)
    }

    suspend fun retryFailedSongs(playlistId: String) {
        val failedSongs = database.playlistSongCrossRefDao()
            .getSongsByStatus(playlistId, SongDownloadStatus.FAILED)

        failedSongs.forEach { crossRef ->
            database.playlistSongCrossRefDao().updateSongStatus(
                playlistId,
                crossRef.songId,
                SongDownloadStatus.PENDING
            )
        }

        // Resume download to retry failed songs
        resumePlaylistDownload(playlistId)
    }

    suspend fun isPlaylistFullyDownloaded(playlistId: String): Boolean {
        val playlist = database.downloadedPlaylistDao().getPlaylistById(playlistId)
        return playlist?.status == PlaylistDownloadStatus.FULL
    }

    suspend fun getPlaylistDownloadState(playlistId: String): DownloadedPlaylistEntity? {
        return database.downloadedPlaylistDao().getPlaylistById(playlistId)
    }

    fun getAllDownloadedPlaylists(): Flow<List<DownloadedPlaylistEntity>> {
        return database.downloadedPlaylistDao().getAllDownloadedPlaylists()
    }

    suspend fun deletePlaylist(playlistId: String) {
        database.downloadedPlaylistDao().deletePlaylist(playlistId)
        database.playlistSongCrossRefDao().deleteSongsForPlaylist(playlistId)
        database.downloadQueueDao().deleteByPlaylistId(playlistId)
        workManager.cancelPlaylistDownload(playlistId)
    }

    // Mobile data preference methods

    suspend fun shouldAllowMobileDataForPlaylist(playlistId: String): Boolean {
        val preference = database.playlistDownloadPreferenceDao().getPreference(playlistId)
        return if (preference != null && preference.expiresAt > System.currentTimeMillis()) {
            preference.allowMobileData
        } else {
            false
        }
    }

    suspend fun setMobileDataPreference(playlistId: String, allowMobileData: Boolean) {
        // Expire after 24 hours
        val expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        val preference = PlaylistDownloadPreferenceEntity(
            playlistId = playlistId,
            allowMobileData = allowMobileData,
            expiresAt = expiresAt
        )
        database.playlistDownloadPreferenceDao().insertPreference(preference)
    }

    suspend fun clearExpiredMobileDataPreferences() {
        database.playlistDownloadPreferenceDao().deleteExpiredPreferences()
    }
}
