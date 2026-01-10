package pe.net.libre.mixtapehaven.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import pe.net.libre.mixtapehaven.data.local.OfflineDatabase
import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import java.io.File

class CacheManager(
    private val database: OfflineDatabase,
    private val dataStoreManager: DataStoreManager,
    private val context: Context
) {
    companion object {
        private const val TAG = "CacheManager"
        private const val DEFAULT_MAX_CACHE_SIZE = 2_000_000_000L // 2GB
    }

    suspend fun getCacheStatistics(): CacheStatistics {
        val downloadedSongs = database.downloadedSongDao().getAllDownloaded().first()
        val totalSize = downloadedSongs.sumOf { it.fileSize + it.imageSize }
        val maxSize = dataStoreManager.maxCacheSize.first() ?: DEFAULT_MAX_CACHE_SIZE

        return CacheStatistics(
            songCount = downloadedSongs.size,
            totalSize = totalSize,
            maxSize = maxSize
        )
    }

    suspend fun evictIfNeeded() {
        val stats = getCacheStatistics()
        if (stats.totalSize > stats.maxSize) {
            val bytesToFree = stats.totalSize - stats.maxSize
            Log.d(TAG, "Cache limit exceeded. Need to free $bytesToFree bytes")
            evictLRU(bytesToFree)
        }
    }

    private suspend fun evictLRU(bytesToFree: Long) {
        var freedBytes = 0L
        val candidates = database.downloadedSongDao().getLeastRecentlyUsed(100)

        Log.d(TAG, "Found ${candidates.size} candidates for eviction")

        for (song in candidates) {
            if (freedBytes >= bytesToFree) break

            Log.d(TAG, "Evicting: ${song.title} (last accessed: ${song.lastAccessTime})")
            deleteSong(song)
            freedBytes += song.fileSize + song.imageSize
        }

        Log.d(TAG, "Eviction complete. Freed $freedBytes bytes")
    }

    suspend fun deleteSong(song: DownloadedSongEntity) {
        try {
            // Delete physical audio file
            val audioFile = File(song.filePath)
            if (audioFile.exists()) {
                val deleted = audioFile.delete()
                Log.d(TAG, "Deleted audio file ${song.filePath}: $deleted")
            }

            // Delete physical image file
            song.imagePath?.let { imagePath ->
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    val deleted = imageFile.delete()
                    Log.d(TAG, "Deleted image file $imagePath: $deleted")
                }
            }

            // Delete database entry
            database.downloadedSongDao().delete(song)
            Log.d(TAG, "Deleted database entry for song: ${song.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting song: ${e.message}", e)
        }
    }

    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing entire cache")

            val allSongs = database.downloadedSongDao().getAllDownloaded().first()
            for (song in allSongs) {
                deleteSong(song)
            }

            Log.d(TAG, "Cache cleared. Deleted ${allSongs.size} songs")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}", e)
        }
    }

    suspend fun cleanupOrphanedFiles() {
        try {
            val musicDir = context.getExternalFilesDir("music")
            val imagesDir = context.getExternalFilesDir("images")

            val downloadedSongs = database.downloadedSongDao().getAllDownloaded().first()
            val validFilePaths = downloadedSongs.map { it.filePath }.toSet()
            val validImagePaths = downloadedSongs.mapNotNull { it.imagePath }.toSet()

            // Clean music directory
            musicDir?.listFiles()?.forEach { file ->
                if (file.absolutePath !in validFilePaths) {
                    Log.d(TAG, "Deleting orphaned music file: ${file.absolutePath}")
                    file.delete()
                }
            }

            // Clean images directory
            imagesDir?.listFiles()?.forEach { file ->
                if (file.absolutePath !in validImagePaths) {
                    Log.d(TAG, "Deleting orphaned image file: ${file.absolutePath}")
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up orphaned files: ${e.message}", e)
        }
    }
}

data class CacheStatistics(
    val songCount: Int,
    val totalSize: Long,
    val maxSize: Long
) {
    val usagePercent: Float
        get() = if (maxSize > 0) {
            (totalSize.toFloat() / maxSize.toFloat()) * 100f
        } else {
            0f
        }

    val availableSize: Long
        get() = (maxSize - totalSize).coerceAtLeast(0)

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    val totalSizeFormatted: String get() = formatSize(totalSize)
    val maxSizeFormatted: String get() = formatSize(maxSize)
    val availableSizeFormatted: String get() = formatSize(availableSize)
}
