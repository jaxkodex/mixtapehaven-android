package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class FileDownloader {
    companion object {
        private const val TAG = "FileDownloader"
    }

    private val okHttpClient: OkHttpClient = createHttpClient()

    /**
     * Download a song with explicit parameters (for WorkManager)
     */
    fun downloadSong(
        serverUrl: String,
        accessToken: String,
        userId: String,
        itemId: String,
        quality: String,
        outputFile: File,
        deviceId: String = "unknown_device",
        onProgress: (Float, Long, Long) -> Unit
    ): Boolean {
        return try {
            // Construct download URL
            val downloadUrl = buildDownloadUrl(serverUrl, itemId, quality)
            Log.d(TAG, "Downloading from: $downloadUrl")

            val authHeader = createAuthorizationHeader(deviceId = deviceId)
            val request = Request.Builder()
                .url(downloadUrl)
                .header("X-Emby-Authorization", authHeader)
                .header("X-Emby-Token", accessToken)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return false
            }

            val body = response.body ?: return false
            val totalBytes = body.contentLength()

            FileOutputStream(outputFile).use { outputStream ->
                body.byteStream().use { inputStream ->
                    var downloadedBytes = 0L
                    val buffer = ByteArray(8192)

                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break

                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read

                        val progress = if (totalBytes > 0) {
                            downloadedBytes.toFloat() / totalBytes.toFloat()
                        } else {
                            0f
                        }
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                }
            }

            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }

    private fun buildDownloadUrl(serverUrl: String, itemId: String, quality: String): String {
        return buildString {
            append(serverUrl)
            if (!serverUrl.endsWith('/')) append('/')
            append("Audio/$itemId/stream")

            val params = when (quality.uppercase()) {
                "ORIGINAL" -> listOf("static=true")
                "HIGH" -> listOf("maxStreamingBitrate=320000", "container=mp3", "audioCodec=mp3")
                "MEDIUM" -> listOf("maxStreamingBitrate=192000", "container=mp3", "audioCodec=mp3")
                "LOW" -> listOf("maxStreamingBitrate=128000", "container=mp3", "audioCodec=mp3")
                else -> listOf("static=true")
            }

            append("?${params.joinToString("&")}")
        }
    }

    suspend fun downloadSong(
        dataStoreManager: DataStoreManager,
        context: Context,
        songId: String,
        quality: StreamingQuality,
        onProgress: (Float, Long, Long) -> Unit
    ): DownloadResult {
        try {
            val serverUrl = dataStoreManager.serverUrl.first()
            val accessToken = dataStoreManager.accessToken.first()

            if (serverUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                return DownloadResult.Failure("Server not configured")
            }

            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            // Construct download URL (same format as streaming URL)
            val downloadUrl = buildString {
                append(serverUrl)
                if (!serverUrl.endsWith('/')) append('/')
                append("Audio/$songId/stream")

                val params = buildList {
                    add("mediaSourceId=$songId")

                    if (quality.useTranscoding) {
                        // Transcoding parameters
                        quality.maxBitrate?.let { add("maxStreamingBitrate=$it") }
                        quality.container?.let {
                            add("container=$it")
                            add("audioCodec=$it")
                        }
                    } else {
                        // Original quality - direct stream
                        add("static=true")
                    }
                }

                append("?${params.joinToString("&")}")
            }

            Log.d(TAG, "Downloading song from: $downloadUrl")

            // Create authenticated request
            val authHeader = createAuthorizationHeader(deviceId = deviceId)
            val request = Request.Builder()
                .url(downloadUrl)
                .header("X-Emby-Authorization", authHeader)
                .header("X-Emby-Token", accessToken)
                .build()

            // Execute download
            val response = okHttpClient.newCall(request).execute()

            Log.d(TAG, "Response code: ${response.code}")

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with code: ${response.code}")
                val errorBody = response.body?.string()
                Log.e(TAG, "Error response: $errorBody")
                return DownloadResult.Failure("Download failed: ${response.code}")
            }

            val body = response.body
            if (body == null) {
                return DownloadResult.Failure("Empty response body")
            }

            val contentType = response.header("Content-Type")
            Log.d(TAG, "Content-Type: $contentType")
            if (contentType != null && !contentType.contains("audio", ignoreCase = true) && !contentType.contains("application/octet-stream", ignoreCase = true)) {
                Log.e(TAG, "Unexpected content type: $contentType (expected audio/*)")
                return DownloadResult.Failure("Server returned non-audio content: $contentType")
            }

            // Get content length for progress tracking
            val totalBytes = body.contentLength()
            Log.d(TAG, "Total bytes to download: $totalBytes")

            // Create output file
            val musicDir = context.getExternalFilesDir("music")
            if (musicDir == null) {
                return DownloadResult.Failure("Cannot access storage")
            }

            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val outputFile = File(musicDir, "$songId.mp3")
            val outputStream = FileOutputStream(outputFile)

            // Download with progress tracking
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)
            val inputStream = body.byteStream()

            try {
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break

                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read

                    // Report progress
                    val progress = if (totalBytes > 0) {
                        downloadedBytes.toFloat() / totalBytes.toFloat()
                    } else {
                        0f
                    }
                    onProgress(progress, downloadedBytes, totalBytes)
                }
            } finally {
                inputStream.close()
                outputStream.close()
            }

            Log.d(TAG, "Download completed: ${outputFile.absolutePath}, size: $downloadedBytes bytes")

            // Verify file integrity
            if (totalBytes > 0 && downloadedBytes != totalBytes) {
                outputFile.delete()
                return DownloadResult.Failure("Download incomplete: $downloadedBytes/$totalBytes bytes")
            }

            return DownloadResult.Success(
                filePath = outputFile.absolutePath,
                size = downloadedBytes,
                format = "mp3"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            return DownloadResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun downloadImage(
        dataStoreManager: DataStoreManager,
        context: Context,
        itemId: String,
        imageType: String = "Primary",
        tag: String
    ): String? {
        try {
            val serverUrl = dataStoreManager.serverUrl.first()
            val accessToken = dataStoreManager.accessToken.first()

            if (serverUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                return null
            }

            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            val imageUrl = buildString {
                append(serverUrl)
                if (!serverUrl.endsWith('/')) append('/')
                append("Items/$itemId/Images/$imageType")
                append("?tag=$tag")
            }

            Log.d(TAG, "Downloading image from: $imageUrl")

            val authHeader = createAuthorizationHeader(deviceId = deviceId)
            val request = Request.Builder()
                .url(imageUrl)
                .header("X-Emby-Authorization", authHeader)
                .header("X-Emby-Token", accessToken)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Image download failed with code: ${response.code}")
                return null
            }

            val body = response.body ?: return null

            // Create output file
            val imagesDir = context.getExternalFilesDir("images")
            if (imagesDir == null) {
                return null
            }

            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val outputFile = File(imagesDir, "$itemId.jpg")
            val outputStream = FileOutputStream(outputFile)

            try {
                body.byteStream().use { input ->
                    input.copyTo(outputStream)
                }
            } finally {
                outputStream.close()
            }

            Log.d(TAG, "Image download completed: ${outputFile.absolutePath}")
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Image download error: ${e.message}", e)
            return null
        }
    }

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun createAuthorizationHeader(
        clientName: String = "Mixtape Haven",
        deviceName: String = "Android",
        deviceId: String,
        version: String = "1.0.0"
    ): String {
        return "MediaBrowser Client=\"$clientName\", Device=\"$deviceName\", " +
                "DeviceId=\"$deviceId\", Version=\"$version\""
    }
}

sealed class DownloadResult {
    data class Success(
        val filePath: String,
        val size: Long,
        val format: String
    ) : DownloadResult()

    data class Failure(val error: String) : DownloadResult()
}
