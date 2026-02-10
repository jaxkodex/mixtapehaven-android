package pe.net.libre.mixtapehaven.data.util

import android.content.Context
import android.os.StatFs

/**
 * Utility class for storage-related operations
 */
object StorageUtil {

    private const val BUFFER_SIZE_MB = 500L * 1024 * 1024 // 500MB buffer

    /**
     * Check if there's enough storage space for a download
     */
    fun hasEnoughSpace(context: Context, requiredBytes: Long): Boolean {
        val availableBytes = getAvailableStorage(context)
        return availableBytes > requiredBytes + BUFFER_SIZE_MB
    }

    /**
     * Get available storage space in bytes
     */
    fun getAvailableStorage(context: Context): Long {
        val path = context.getExternalFilesDir(null)?.path
            ?: context.filesDir.path
        val stat = StatFs(path)
        return stat.availableBytes
    }

    /**
     * Get total storage space in bytes
     */
    fun getTotalStorage(context: Context): Long {
        val path = context.getExternalFilesDir(null)?.path
            ?: context.filesDir.path
        val stat = StatFs(path)
        return stat.totalBytes
    }

    /**
     * Format bytes to human-readable string
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Calculate required space for a playlist
     */
    fun calculatePlaylistSize(songSizes: List<Long>): Long {
        return songSizes.sum()
    }
}
