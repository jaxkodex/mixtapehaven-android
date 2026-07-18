package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.model.VideoItem
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

/**
 * Progress of one in-flight video download. [percent] stays 0 while the server transcodes with an
 * unknown final size, so surface [bytes] ("142 MB so far") rather than a determinate bar.
 */
data class VideoDownloadProgress(val percent: Int, val bytes: Long)

/**
 * App-scoped service that saves quality-capped transcodes of movies/episodes for offline playback.
 * Unlike the audio [DownloadManager] nothing is saved automatically: downloads are user-triggered
 * per title via [download]. The quality cap comes from [DownloadSettingsStore.videoQuality] at
 * download time. An incomplete row is kept while bytes stream so the UI can show in-flight state;
 * failures and cancellations clean up both the partial file and the row.
 */
class VideoDownloadManager(
    context: Context,
    private val repository: JellyfinRepository,
    private val dao: VideoDownloadDao,
    private val settingsStore: DownloadSettingsStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val downloadsDir: File = File(context.applicationContext.filesDir, DIR).apply { mkdirs() }

    /** Saved + in-flight video downloads, observed by the Downloads/Detail UI. */
    val downloads: Flow<List<DownloadedVideo>> = dao.observeAll()

    /** Running total of bytes used by completed video downloads. */
    val totalSizeBytes: Flow<Long> = dao.observeTotalSize()

    private val _progress = MutableStateFlow<Map<String, VideoDownloadProgress>>(emptyMap())

    /** In-flight download progress keyed by item id; an id is absent once its download ends. */
    val progress: StateFlow<Map<String, VideoDownloadProgress>> = _progress.asStateFlow()

    /** Completed downloads as id -> file path, kept in memory so the resolver lookup is synchronous. */
    @Volatile
    private var completedPaths: Map<String, String> = emptyMap()

    private val activeJobs: MutableMap<String, Job> = Collections.synchronizedMap(mutableMapOf())

    init {
        scope.launch {
            dao.observeAll().collect { rows ->
                completedPaths = rows.filter { it.complete }.associate { it.id to it.filePath }
            }
        }
    }

    /**
     * Local playback URI for [itemId] if a completed copy exists, else null. Synchronous
     * (in-memory lookup) so it can back [pe.net.libre.mixtapehaven.di.AppContainer.videoSourceResolver].
     */
    fun localUriFor(itemId: String): String? {
        val path = completedPaths[itemId] ?: return null
        val file = File(path)
        return if (file.exists()) Uri.fromFile(file).toString() else null
    }

    /** Usable bytes remaining on the volume backing the download directory. */
    fun usableSpaceBytes(): Long = downloadsDir.usableSpace

    /** Start downloading [item] (a movie or episode). No-op if already saved or in flight. */
    fun download(item: VideoItem) {
        val id = item.id
        if (completedPaths.containsKey(id)) return
        synchronized(activeJobs) {
            if (activeJobs.containsKey(id)) return
            activeJobs[id] = scope.launch {
                try {
                    runCatching { performDownload(item, id) }
                        .onFailure { Log.w(TAG, "Video download failed for $id", it) }
                } finally {
                    // Also runs on success, where the row is complete and the cleanup no-ops.
                    withContext(NonCancellable) { cleanupIfIncomplete(id) }
                    activeJobs.remove(id)
                    _progress.update { it - id }
                }
            }
        }
    }

    /** Cancel an in-flight download of [id], or delete its saved copy. */
    suspend fun remove(id: String) {
        activeJobs[id]?.let { job ->
            job.cancel()
            runCatching { job.join() }
        }
        dao.findById(id)?.let { deleteQuietly(File(it.filePath)) }
        deleteQuietly(File(downloadsDir, "$id.mp4"))
        dao.deleteById(id)
    }

    /** Cancel in-flight downloads, delete every saved video, and clear the video library. */
    suspend fun removeAll() {
        val jobs = synchronized(activeJobs) {
            activeJobs.values.toList().also { activeJobs.clear() }
        }
        jobs.forEach { it.cancel() }
        jobs.forEach { runCatching { it.join() } }
        _progress.value = emptyMap()
        dao.clear()
        withContext(Dispatchers.IO) {
            downloadsDir.listFiles()?.forEach { runCatching { it.delete() } }
        }
        completedPaths = emptyMap()
    }

    private suspend fun performDownload(item: VideoItem, id: String) {
        val quality = settingsStore.videoQuality.first()
        val url = transcodeUrlOrNull(id, quality) ?: return
        // Incomplete row first, so the Downloads UI lists the title while bytes stream in.
        dao.upsert(item.toDownloadedVideo(path = "", size = 0, qualityLabel = quality.label, complete = false))
        _progress.update { it + (id to VideoDownloadProgress(percent = 0, bytes = 0)) }
        val target = File(downloadsDir, "$id.mp4")
        val part = File(downloadsDir, "$id.mp4.part")
        val saved = streamToFile(url, part, id)
        if (saved && part.renameTo(target)) {
            dao.upsert(
                item.toDownloadedVideo(
                    path = target.path,
                    size = target.length(),
                    qualityLabel = quality.label,
                    complete = true,
                ),
            )
        }
    }

    /** The transcode URL to download for [id], or null if it is already saved or storage is low. */
    private suspend fun transcodeUrlOrNull(id: String, quality: VideoDownloadQuality): String? {
        val alreadySaved = dao.findById(id)?.complete == true
        val lowStorage = !shouldStartDownload(usableSpaceBytes(), MIN_FREE_BYTES)
        if (lowStorage) Log.w(TAG, "Skipping video download of $id: storage low")
        return if (alreadySaved || lowStorage) {
            null
        } else {
            repository.videoDownloadUrl(
                itemId = id,
                maxHeight = quality.maxHeight,
                videoBitRate = quality.videoBitRate,
                audioBitRate = quality.audioBitRate,
            )
        }
    }

    /** Delete the partial file and the incomplete row of [id]; no-op once the download completed. */
    private suspend fun cleanupIfIncomplete(id: String) {
        if (dao.findById(id)?.complete != true) {
            deleteQuietly(File(downloadsDir, "$id.mp4.part"))
            dao.deleteById(id)
        }
    }

    /** Stream [url] into [part], emitting progress for [id]. Returns false if cancelled or on HTTP error. */
    private suspend fun streamToFile(url: String, part: File, id: String): Boolean {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Video download of $id got HTTP ${response.code}")
                false
            } else {
                copyBody(response.body, part, id)
            }
        }
    }

    /** Copy [body] to [part], emitting progress for [id]. Returns false if cancelled mid-stream. */
    private suspend fun copyBody(body: ResponseBody, part: File, id: String): Boolean =
        part.outputStream().use { output ->
            body.byteStream().use { input -> pumpStream(input, output, body.contentLength(), id) }
        }

    /** Pump [input] to [output] in chunks, emitting progress. False if cancelled before completion. */
    private suspend fun pumpStream(input: InputStream, output: OutputStream, total: Long, id: String): Boolean {
        val buffer = ByteArray(BUFFER_BYTES)
        var downloaded = 0L
        while (currentCoroutineContext().isActive) {
            val read = input.read(buffer)
            if (read < 0) return true
            output.write(buffer, 0, read)
            downloaded += read
            emitProgress(id, downloaded, total)
        }
        return false
    }

    /** Publish progress when the percent changes or another [PROGRESS_STEP_BYTES] have arrived. */
    private fun emitProgress(id: String, downloaded: Long, total: Long) {
        val previous = _progress.value[id]
        val percent = percentOf(downloaded, total)
        if (previous == null || percent != previous.percent || downloaded - previous.bytes >= PROGRESS_STEP_BYTES) {
            _progress.update { it + (id to VideoDownloadProgress(percent, downloaded)) }
        }
    }

    private fun deleteQuietly(file: File) {
        if (file.exists() && !file.delete()) Log.w(TAG, "Could not delete ${file.name}")
    }

    private fun VideoItem.toDownloadedVideo(
        path: String,
        size: Long,
        qualityLabel: String,
        complete: Boolean,
    ) = DownloadedVideo(
        id = id,
        title = title,
        kind = kind.name,
        seriesName = seriesName,
        seasonEpisodeLabel = seasonEpisodeLabel,
        runtimeLabel = runtimeLabel,
        posterUrl = posterUrl,
        artColorArgb = artColor.toArgb(),
        qualityLabel = qualityLabel,
        filePath = path,
        sizeBytes = size,
        complete = complete,
    )

    private companion object {
        const val TAG = "VideoDownloadManager"
        const val DIR = "video_downloads"
        const val BUFFER_BYTES = 64 * 1024
        const val PROGRESS_STEP_BYTES = 512L * 1024
        const val MIN_FREE_BYTES = 500L * 1024 * 1024
    }
}
