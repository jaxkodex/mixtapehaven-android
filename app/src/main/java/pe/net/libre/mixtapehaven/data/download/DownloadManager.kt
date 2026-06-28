package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.model.Track
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

/** Progress of the download currently in flight: the [track] being saved and its [percent] (0-100). */
data class DownloadProgress(val track: Track, val percent: Int)

/**
 * App-scoped service that saves the original file bytes of tracks as they play, so the library
 * becomes available offline. Observes [PlayerController.nowPlaying]; when auto-download is enabled
 * it streams the static original file from [repository] to internal storage and records a
 * [DownloadedTrack] row. Failures never crash playback and already-saved tracks are skipped.
 */
class DownloadManager(
    context: Context,
    private val repository: JellyfinRepository,
    private val dao: DownloadDao,
    private val settingsStore: DownloadSettingsStore,
    private val playerController: PlayerController,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val appContext = context.applicationContext
    private val downloadsDir: File = File(appContext.filesDir, DOWNLOADS_DIR).apply { mkdirs() }

    /** Saved + in-progress downloads, observed by the Downloads/Home UI. */
    val downloads: Flow<List<DownloadedTrack>> = dao.observeAll()

    /** Running total of bytes used by saved downloads. */
    val totalSizeBytes: Flow<Long> = dao.observeTotalSize()

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    @Volatile
    private var autoDownloadEnabled: Boolean = true

    /** Completed downloads as id -> file path, kept in memory so the resolver lookup is synchronous. */
    @Volatile
    private var completedPaths: Map<String, String> = emptyMap()

    private val inProgress: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    /** Begin observing settings, the saved library, and playback. Idempotent per instance. */
    fun start() {
        scope.launch { settingsStore.autoDownloadEnabled.collect { autoDownloadEnabled = it } }
        scope.launch {
            dao.observeAll().collect { rows ->
                completedPaths = rows.filter { it.complete }.associate { it.id to it.filePath }
            }
        }
        scope.launch {
            playerController.nowPlaying.collect { track -> track?.let(::onTrackPlaying) }
        }
    }

    /**
     * Local playback URI for [track] if its bytes are saved, else null. Synchronous (in-memory
     * lookup) so it can back [PlayerController.streamUrlResolver] when media items are built.
     */
    fun localUriFor(track: Track): String? {
        val path = track.id?.let { completedPaths[it] } ?: return null
        val file = File(path)
        return if (file.exists()) Uri.fromFile(file).toString() else null
    }

    /** Usable bytes remaining on the volume backing the download directory. */
    fun usableSpaceBytes(): Long = downloadsDir.usableSpace

    /** Delete every saved file and clear the download library. */
    suspend fun removeAll() {
        val rows = dao.getAll()
        dao.clear()
        withContext(Dispatchers.IO) {
            rows.forEach { runCatching { File(it.filePath).delete() } }
        }
        completedPaths = emptyMap()
        _progress.value = null
    }

    private fun onTrackPlaying(track: Track) {
        val id = track.id
        if (!autoDownloadEnabled || id == null) return
        if (completedPaths.containsKey(id) || !inProgress.add(id)) return
        scope.launch {
            try {
                runCatching { download(track, id) }
                    .onFailure { Log.w(TAG, "Download failed for $id", it) }
            } finally {
                inProgress.remove(id)
                if (_progress.value?.track?.id == id) _progress.value = null
            }
        }
    }

    private suspend fun download(track: Track, id: String) {
        val url = downloadUrlOrNull(id) ?: return
        val target = File(downloadsDir, id)
        val part = File(downloadsDir, "$id.part")
        val saved = runCatching { streamToFile(url, part, track) }
            .onFailure { part.delete() }
            .getOrThrow()
        if (saved && part.renameTo(target)) {
            dao.upsert(track.toDownloadedTrack(id, target.path, target.length()))
        } else {
            part.delete()
        }
    }

    /** The stream URL to download for [id], or null if it is already saved or storage is low. */
    private suspend fun downloadUrlOrNull(id: String): String? {
        val alreadySaved = dao.findById(id)?.complete == true
        val lowStorage = !shouldStartDownload(usableSpaceBytes(), MIN_FREE_BYTES)
        if (lowStorage) Log.w(TAG, "Skipping download of $id: storage low")
        return if (alreadySaved || lowStorage) null else repository.audioStreamUrl(id)
    }

    /** Stream [url] into [part], emitting progress for [track]. Returns false if cancelled mid-stream. */
    private suspend fun streamToFile(url: String, part: File, track: Track): Boolean {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) false else copyToFile(body, part, track)
        }
    }

    /** Copy [body] to [part], emitting progress for [track]. Returns false if cancelled mid-stream. */
    private suspend fun copyToFile(body: ResponseBody, part: File, track: Track): Boolean {
        val total = body.contentLength()
        return part.outputStream().use { output ->
            body.byteStream().use { input -> pumpStream(input, output, total, track) }
        }
    }

    /** Pump [input] to [output] in chunks, emitting progress. False if cancelled before completion. */
    private suspend fun pumpStream(
        input: InputStream,
        output: OutputStream,
        total: Long,
        track: Track,
    ): Boolean {
        val buffer = ByteArray(BUFFER_BYTES)
        var downloaded = 0L
        var lastPercent = -1
        while (currentCoroutineContext().isActive) {
            val read = input.read(buffer)
            if (read < 0) return true
            output.write(buffer, 0, read)
            downloaded += read
            lastPercent = emitProgress(track, downloaded, total, lastPercent)
        }
        return false
    }

    /** Emit a [DownloadProgress] update for [track] when the percent changes; returns the new percent. */
    private fun emitProgress(track: Track, downloaded: Long, total: Long, lastPercent: Int): Int {
        val percent = percentOf(downloaded, total)
        if (percent != lastPercent) _progress.value = DownloadProgress(track, percent)
        return percent
    }

    private fun Track.toDownloadedTrack(id: String, path: String, size: Long) = DownloadedTrack(
        id = id,
        title = title,
        artist = artist,
        durationLabel = durationLabel,
        imageUrl = imageUrl,
        artColorArgb = artColor.toArgb(),
        filePath = path,
        sizeBytes = size,
        complete = true,
    )

    private companion object {
        const val TAG = "DownloadManager"
        const val DOWNLOADS_DIR = "downloads"
        const val BUFFER_BYTES = 64 * 1024
        const val MIN_FREE_BYTES = 200L * 1024 * 1024
    }
}

/**
 * Local-first URL resolution: the saved local URI from [localUri] when present, else the remote
 * stream from [streamUrl]. Pure (no Android/file deps) so the resolver seam is unit-coverable.
 */
internal fun resolveLocalFirst(
    track: Track,
    localUri: (Track) -> String?,
    streamUrl: (String) -> String?,
): String? = localUri(track) ?: track.id?.let(streamUrl)

/** True when enough free space remains to start a download (keeps at least [minFreeBytes] free). */
internal fun shouldStartDownload(freeBytes: Long, minFreeBytes: Long): Boolean =
    freeBytes > minFreeBytes

/** Download percent (0-100) given bytes [downloaded] of [total]; 0 when total is unknown. */
internal fun percentOf(downloaded: Long, total: Long): Int =
    if (total <= 0) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)

/** Human-readable byte size, e.g. "9.8 MB" or "1.2 GB". */
internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / 1_000_000.0
    return when {
        mb >= 1000 -> "%.1f GB".format(mb / 1000)
        mb >= 10 -> "%.0f MB".format(mb)
        else -> "%.1f MB".format(mb)
    }
}
