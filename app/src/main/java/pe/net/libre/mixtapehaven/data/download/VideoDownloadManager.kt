package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Progress of one in-flight video download. [percent] stays 0 while the server transcodes with an
 * unknown final size, so surface [bytes] ("142 MB so far") rather than a determinate bar.
 */
data class VideoDownloadProgress(val percent: Int, val bytes: Long)

/** What [VideoDownloadManager.runDownload] concluded, for [VideoDownloadWorker] to map to a Result. */
enum class VideoDownloadOutcome {
    /** File fully saved and the row marked complete. */
    COMPLETED,

    /** Transient failure (network blip, server 5xx): worth retrying with backoff. */
    RETRY,

    /** Permanent failure (client error, low storage): row already marked failed, don't retry. */
    FAILED,

    /** Nothing to do — the row was removed while queued, or the file is already saved. */
    SKIPPED,
}

/**
 * App-scoped service that saves quality-capped transcodes of movies/episodes for offline playback.
 * Unlike the audio [DownloadManager] nothing is saved automatically: downloads are user-triggered
 * per title via [download]. The quality cap comes from [DownloadSettingsStore.videoQuality] at
 * download time.
 *
 * Transfers themselves run inside [VideoDownloadWorker] via WorkManager, which gives the queue
 * persistence across process death, a Wi-Fi-only network constraint, and retry with backoff.
 * [download] only writes a QUEUED row (so the UI lists the title immediately) and enqueues;
 * the worker calls back into [runDownload] for the actual streaming.
 *
 * Note a retry always restarts from byte 0: the source is a live server-side transcode whose
 * bytes are not stable across ffmpeg runs, so resuming a .part from a different run would splice
 * two streams into a corrupt file.
 */
class VideoDownloadManager(
    context: Context,
    private val repository: JellyfinRepository,
    private val dao: VideoDownloadDao,
    private val settingsStore: DownloadSettingsStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val appContext = context.applicationContext
    private val downloadsDir: File = File(appContext.filesDir, DIR).apply { mkdirs() }

    // Lazy so unit tests can construct the manager without a WorkManager-initialized context.
    private val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    /** Saved + in-flight video downloads, observed by the Downloads/Detail UI. */
    val downloads: Flow<List<DownloadedVideo>> = dao.observeAll()

    /** Running total of bytes used by completed video downloads. */
    val totalSizeBytes: Flow<Long> = dao.observeTotalSize()

    private val _progress = MutableStateFlow<Map<String, VideoDownloadProgress>>(emptyMap())

    /** In-flight download progress keyed by item id; an id is absent once its download ends. */
    val progress: StateFlow<Map<String, VideoDownloadProgress>> = _progress.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = ERROR_BUFFER)

    /** One human-readable message per failed download, for the UI to toast/snackbar. */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    /** Completed downloads as id -> file path, kept in memory so the resolver lookup is synchronous. */
    @Volatile
    private var completedPaths: Map<String, String> = emptyMap()

    init {
        scope.launch {
            dao.observeAll().collect { rows ->
                completedPaths = rows.filter { it.complete }.associate { it.id to it.filePath }
            }
        }
        scope.launch { runCatching { reconcileQueue() }.onFailure { Log.w(TAG, "Queue reconcile failed", it) } }
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

    /** True when the active network bills by the byte, i.e. queued work is waiting for Wi-Fi. */
    fun isNetworkMetered(): Boolean =
        appContext.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true

    /**
     * Queue [item] (a movie or episode) for download. No-op if already saved or queued; re-taps
     * on a failed row re-queue it. The transfer starts when its worker gets network + its turn.
     */
    fun download(item: VideoItem) {
        val id = item.id
        if (completedPaths.containsKey(id)) return
        scope.launch {
            if (dao.findById(id)?.complete == true) return@launch
            val quality = settingsStore.videoQuality.first()
            // QUEUED row first, so the Downloads UI lists the title before any bytes stream.
            dao.upsert(
                item.toDownloadedVideo(
                    path = "",
                    size = 0,
                    qualityLabel = quality.label,
                    complete = false,
                    status = VideoDownloadStatus.QUEUED,
                ),
            )
            enqueue(id)
        }
    }

    /** Re-queue a failed download. No-op if the row is gone or already saved. */
    fun retry(id: String) {
        scope.launch {
            val row = dao.findById(id) ?: return@launch
            if (row.complete) return@launch
            dao.updateStatus(id, VideoDownloadStatus.QUEUED.name)
            enqueue(id)
        }
    }

    /** Cancel an in-flight/queued download of [id], or delete its saved copy. */
    suspend fun remove(id: String) {
        workManager.cancelUniqueWork(uniqueWorkName(id))
        dao.findById(id)?.takeIf { it.filePath.isNotEmpty() }?.let { deleteQuietly(File(it.filePath)) }
        deleteQuietly(File(downloadsDir, "$id$FILE_EXT"))
        deleteQuietly(File(downloadsDir, "$id$FILE_EXT.part"))
        dao.deleteById(id)
        _progress.update { it - id }
    }

    /** Cancel all queued/in-flight downloads, delete every saved video, and clear the video library. */
    suspend fun removeAll() {
        workManager.cancelAllWorkByTag(WORK_TAG)
        _progress.value = emptyMap()
        dao.clear()
        withContext(Dispatchers.IO) {
            downloadsDir.listFiles()?.forEach { runCatching { it.delete() } }
        }
        completedPaths = emptyMap()
    }

    /**
     * Run the download of [id] end to end: resolve the transcode URL, stream to a .part file,
     * and promote it to the final file + COMPLETE row. Called from [VideoDownloadWorker] only.
     * On [VideoDownloadOutcome.RETRY] the row is left QUEUED for the backoff re-run; on
     * [VideoDownloadOutcome.FAILED] it is already marked failed and the error emitted.
     */
    suspend fun runDownload(id: String): VideoDownloadOutcome {
        val row = dao.findById(id)
        if (row == null || row.complete) return VideoDownloadOutcome.SKIPPED
        val part = File(downloadsDir, "$id$FILE_EXT.part")
        return try {
            if (shouldStartDownload(usableSpaceBytes(), MIN_FREE_BYTES)) {
                streamAtQuality(row, settingsStore.videoQuality.first(), part)
            } else {
                Log.w(TAG, "Skipping video download of ${row.id}: storage low")
                failPermanently(row.id, "Not enough free space to download")
            }
        } catch (failure: IOException) {
            // OkHttp surfaces our call.cancel() (worker stopped) as an IOException too; a genuine
            // network failure in a live coroutine is the retryable case.
            if (!currentCoroutineContext().isActive) throw CancellationException("worker stopped")
            Log.w(TAG, "Video download failed for $id", failure)
            withContext(NonCancellable) { dao.updateStatus(id, VideoDownloadStatus.QUEUED.name) }
            VideoDownloadOutcome.RETRY
        } finally {
            withContext(NonCancellable) {
                val after = dao.findById(id)
                if (after?.complete != true) deleteQuietly(part)
                // Only cancellation (worker stopped, network constraint lost) exits with the row
                // still RUNNING — every outcome writes its own status first. Drop it back to
                // QUEUED so the UI shows "Waiting…" instead of a dead spinner until the re-run.
                if (after != null && !after.complete && after.status == VideoDownloadStatus.RUNNING.name) {
                    dao.updateStatus(id, VideoDownloadStatus.QUEUED.name)
                }
                _progress.update { it - id }
            }
        }
    }

    private suspend fun streamAtQuality(
        row: DownloadedVideo,
        quality: VideoDownloadQuality,
        part: File,
    ): VideoDownloadOutcome {
        val url = repository.videoDownloadUrl(
            itemId = row.id,
            maxHeight = quality.maxHeight,
            videoBitRate = quality.videoBitRate,
            audioBitRate = quality.audioBitRate,
        )
        if (url == null) {
            // No session/connectivity yet: transient, the backoff re-run will find it.
            dao.updateStatus(row.id, VideoDownloadStatus.QUEUED.name)
            return VideoDownloadOutcome.RETRY
        }
        dao.updateStatus(row.id, VideoDownloadStatus.RUNNING.name)
        _progress.update { it + (row.id to VideoDownloadProgress(percent = 0, bytes = 0)) }
        return when (val streamed = streamToFile(url, part, row.id)) {
            StreamResult.SAVED -> finalizeDownload(row, quality, part)
            is StreamResult.HttpError -> httpOutcome(row.id, streamed.code)
        }
    }

    /** Promote the fully-streamed [part] to the final file and mark the row complete. */
    private suspend fun finalizeDownload(
        row: DownloadedVideo,
        quality: VideoDownloadQuality,
        part: File,
    ): VideoDownloadOutcome {
        val target = File(downloadsDir, "${row.id}$FILE_EXT")
        return if (part.renameTo(target)) {
            dao.upsert(
                row.copy(
                    filePath = target.path,
                    sizeBytes = target.length(),
                    qualityLabel = quality.label,
                    complete = true,
                    status = VideoDownloadStatus.COMPLETE.name,
                ),
            )
            VideoDownloadOutcome.COMPLETED
        } else {
            failPermanently(row.id, "Download failed for ${row.title}")
        }
    }

    /** 4xx means the request itself is wrong and will stay wrong; anything else is worth a retry. */
    private suspend fun httpOutcome(id: String, code: Int): VideoDownloadOutcome =
        if (code in CLIENT_ERRORS) {
            failPermanently(id, "Download failed (server error $code)")
        } else {
            dao.updateStatus(id, VideoDownloadStatus.QUEUED.name)
            VideoDownloadOutcome.RETRY
        }

    /** Mark [id] failed after the worker exhausted its retries, emitting the user-facing error. */
    suspend fun markFailed(id: String) {
        val title = dao.findById(id)?.title
        failPermanently(id, if (title != null) "Download failed for $title" else "Download failed")
    }

    /** Enqueue the unique worker for [id]; KEEP so a re-tap while queued/running is a no-op. */
    private suspend fun enqueue(id: String) {
        val wifiOnly = settingsStore.wifiOnly.first()
        // The constraint is snapshotted at enqueue time: flipping the Wi-Fi-only setting later
        // affects new enqueues, not work already waiting.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(workDataOf(VideoDownloadWorker.KEY_ITEM_ID to id))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(uniqueWorkName(id), ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Reconcile rows against WorkManager's queue at startup: a row with no live work (work
     * pruned, data cleared) becomes FAILED so the UI offers retry instead of a stuck row, and a
     * RUNNING row whose worker died with the process drops back to QUEUED until its re-run streams.
     */
    private suspend fun reconcileQueue() {
        for (id in dao.incompleteIds()) {
            val alive = withContext(Dispatchers.IO) {
                runCatching { workManager.getWorkInfosForUniqueWork(uniqueWorkName(id)).get() }.getOrNull()
            }?.any { !it.state.isFinished }
            val row = dao.findById(id)
            when {
                alive == null || row == null -> Unit // WorkManager unavailable / row deleted meanwhile
                !alive -> dao.updateStatus(id, VideoDownloadStatus.FAILED.name)
                row.status == VideoDownloadStatus.RUNNING.name ->
                    dao.updateStatus(id, VideoDownloadStatus.QUEUED.name)
            }
        }
    }

    private suspend fun failPermanently(id: String, message: String): VideoDownloadOutcome {
        withContext(NonCancellable) { dao.updateStatus(id, VideoDownloadStatus.FAILED.name) }
        _errors.tryEmit(message)
        return VideoDownloadOutcome.FAILED
    }

    /** How one streaming attempt ended (cancellation propagates as an exception instead). */
    private sealed interface StreamResult {
        data object SAVED : StreamResult
        data class HttpError(val code: Int) : StreamResult
    }

    /** Stream [url] into [part], emitting progress for [id]. */
    private suspend fun streamToFile(url: String, part: File, id: String): StreamResult {
        val call = client.newCall(Request.Builder().url(url).build())
        // Blocking OkHttp I/O ignores coroutine cancellation; without this the socket keeps
        // pulling the transcode until it times out after the worker is stopped.
        val onCancel = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        return try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Video download of $id got HTTP ${response.code}")
                    StreamResult.HttpError(response.code)
                } else {
                    copyBody(response.body, part, id)
                    StreamResult.SAVED
                }
            }
        } finally {
            onCancel?.dispose()
        }
    }

    /** Copy [body] to [part], emitting progress for [id]. */
    private suspend fun copyBody(body: ResponseBody, part: File, id: String) {
        part.outputStream().use { output ->
            body.byteStream().use { input -> pumpStream(input, output, body.contentLength(), id) }
        }
    }

    /** Pump [input] to [output] in chunks, emitting progress. Throws if cancelled mid-stream. */
    private suspend fun pumpStream(input: InputStream, output: OutputStream, total: Long, id: String) {
        val buffer = ByteArray(BUFFER_BYTES)
        var downloaded = 0L
        while (true) {
            if (!currentCoroutineContext().isActive) throw CancellationException("download cancelled")
            val read = input.read(buffer)
            if (read < 0) return
            output.write(buffer, 0, read)
            downloaded += read
            emitProgress(id, downloaded, total)
        }
    }

    /** Publish progress when the percent changes or another [PROGRESS_STEP_BYTES] have arrived. */
    private fun emitProgress(id: String, downloaded: Long, total: Long) {
        val previous = _progress.value[id]
        val percent = percentOf(downloaded, total)
        if (previous == null || percent != previous.percent || downloaded - previous.bytes >= PROGRESS_STEP_BYTES) {
            _progress.update { it + (id to VideoDownloadProgress(percent, downloaded)) }
        }
    }

    companion object {
        private const val TAG = "VideoDownloadManager"
        private const val DIR = "video_downloads"

        /** MPEG-TS: the only container the server can finalize over a non-seekable HTTP response. */
        private const val FILE_EXT = ".ts"
        private const val BUFFER_BYTES = 64 * 1024
        private const val PROGRESS_STEP_BYTES = 512L * 1024
        private const val MIN_FREE_BYTES = 500L * 1024 * 1024
        private const val ERROR_BUFFER = 4
        private const val BACKOFF_SECONDS = 30L
        private val CLIENT_ERRORS = 400..499

        /** Tag on every video download work request, for bulk cancellation. */
        const val WORK_TAG = "video-download"

        /** Unique WorkManager name for the download of [id], shared by enqueue/cancel/reconcile. */
        fun uniqueWorkName(id: String): String = "video-download-$id"
    }
}

private fun deleteQuietly(file: File) {
    if (file.exists() && !file.delete()) Log.w("VideoDownloadManager", "Could not delete ${file.name}")
}

private fun VideoItem.toDownloadedVideo(
    path: String,
    size: Long,
    qualityLabel: String,
    complete: Boolean,
    status: VideoDownloadStatus,
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
    status = status.name,
)
