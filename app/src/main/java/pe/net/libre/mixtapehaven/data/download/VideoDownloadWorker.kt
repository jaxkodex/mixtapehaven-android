package pe.net.libre.mixtapehaven.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.net.libre.mixtapehaven.MixtapeApplication
import pe.net.libre.mixtapehaven.R

/**
 * Runs one video download end to end by calling back into [VideoDownloadManager.runDownload].
 * WorkManager gives the transfer what the old self-managed coroutine lacked: the queue survives
 * process death (the worker simply re-runs), transient failures retry with exponential backoff
 * (capped at [MAX_ATTEMPTS] — each retry restarts from byte 0, transcodes are not resumable),
 * and the network constraint set at enqueue time implements Wi-Fi-only for free.
 *
 * Runs as a dataSync foreground service for the duration: Android freezes cached processes on
 * screen-off, which aborts these multi-minute sockets. The mandatory notification doubles as the
 * user's progress cue and is updated with streamed bytes.
 */
class VideoDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val manager: VideoDownloadManager
        get() = (applicationContext as MixtapeApplication).container.videoDownloadManager

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        // Foreground before any waiting/streaming. WorkManager is exempt from the
        // background-FGS-start restriction, so this works even when the process was relaunched
        // in the background to resume the queue.
        runCatching { setForeground(foregroundInfo()) }
            .onFailure { Log.w(TAG, "Could not enter foreground; downloading best-effort", it) }
        // One transcode at a time: N queued items would otherwise run N concurrent server-side
        // ffmpeg sessions. Later workers wait here; their rows stay QUEUED ("Waiting…").
        return TRANSCODE_MUTEX.withLock {
            coroutineScope {
                val notifier = launch { publishProgress(id) }
                try {
                    toResult(manager.runDownload(id), id)
                } finally {
                    notifier.cancel()
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo()

    private suspend fun toResult(outcome: VideoDownloadOutcome, id: String): Result = when (outcome) {
        VideoDownloadOutcome.COMPLETED, VideoDownloadOutcome.SKIPPED -> Result.success()
        VideoDownloadOutcome.FAILED -> Result.failure()
        VideoDownloadOutcome.RETRY ->
            // runAttemptCount is 0-based, so this allows MAX_ATTEMPTS runs in total.
            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                Result.retry()
            } else {
                manager.markFailed(id)
                Result.failure()
            }
    }

    /** Mirror streamed bytes into the foreground notification while the download runs. */
    private suspend fun publishProgress(id: String) {
        manager.progress
            .map { it[id]?.bytes ?: 0L }
            .distinctUntilChanged()
            .collect { bytes ->
                if (bytes > 0) notify(buildNotification("${formatBytes(bytes)} so far"))
            }
    }

    private fun notify(notification: Notification) {
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun foregroundInfo(): ForegroundInfo {
        createChannel()
        return ForegroundInfo(
            NOTIFICATION_ID,
            buildNotification("Saving for offline playback"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun createChannel() {
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Video downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading video")
            .setContentText(text)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "VideoDownloadWorker"
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID = 20
        private const val MAX_ATTEMPTS = 5

        /** Serializes transfers process-wide so the server only ever runs one transcode for us. */
        private val TRANSCODE_MUTEX = Mutex()

        /** Input-data key carrying the item id to download. */
        const val KEY_ITEM_ID = "item_id"
    }
}
