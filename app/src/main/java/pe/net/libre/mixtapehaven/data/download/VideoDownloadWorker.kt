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
import kotlin.math.absoluteValue

/**
 * Runs one video download end to end by calling back into [VideoDownloadManager.runDownload].
 * WorkManager gives the transfer what the old self-managed coroutine lacked: the queue survives
 * process death (the worker simply re-runs), transient failures retry with exponential backoff,
 * and the network constraint set at enqueue time implements Wi-Fi-only for free. The retry
 * budget is the row's `attempts` column, owned by the manager — WorkManager's `runAttemptCount`
 * also ticks up on constraint stops, which would spend the budget of a download that never failed.
 * Each retry restarts from byte 0; transcodes are not resumable.
 *
 * Runs as a dataSync foreground service while it actually streams: Android freezes cached
 * processes on screen-off, which aborts these multi-minute sockets. The mandatory notification
 * doubles as the user's progress cue and is updated with streamed bytes.
 */
class VideoDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val manager: VideoDownloadManager
        get() = (applicationContext as MixtapeApplication).container.videoDownloadManager

    private val itemId: String? get() = inputData.getString(KEY_ITEM_ID)

    override suspend fun doWork(): Result {
        val id = itemId ?: return Result.failure()
        // One transcode at a time: N queued items would otherwise run N concurrent server-side
        // ffmpeg sessions. Later workers wait here; their rows stay QUEUED ("Waiting…"). Waiting
        // workers don't need their own foreground — the active worker's FGS keeps the shared
        // process unfrozen — and going foreground only past the lock means the queue never
        // stacks idle "Saving for offline playback" notifications over the live progress one.
        return TRANSCODE_MUTEX.withLock {
            // WorkManager is exempt from the background-FGS-start restriction, so this works even
            // when the process was relaunched in the background to resume the queue.
            runCatching { setForeground(foregroundInfo()) }
                .onFailure { Log.w(TAG, "Could not enter foreground; downloading best-effort", it) }
            coroutineScope {
                val notifier = launch { publishProgress(id) }
                try {
                    when (manager.runDownload(id)) {
                        VideoDownloadOutcome.COMPLETED, VideoDownloadOutcome.SKIPPED -> Result.success()
                        VideoDownloadOutcome.RETRY -> Result.retry()
                        VideoDownloadOutcome.FAILED -> Result.failure()
                    }
                } finally {
                    notifier.cancel()
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo()

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
            .notify(notificationId(), notification)
    }

    private fun foregroundInfo(): ForegroundInfo {
        createChannel()
        return ForegroundInfo(
            notificationId(),
            buildNotification("Saving for offline playback"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    /**
     * Per-item notification id. With a shared id, worker A finishing would tear down worker B's
     * notification/foreground association — WorkManager's SystemForegroundService cleans up by id.
     */
    private fun notificationId(): Int =
        NOTIFICATION_ID_BASE + ((itemId?.hashCode() ?: 0).absoluteValue % NOTIFICATION_ID_RANGE)

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

        /** Ids [NOTIFICATION_ID_BASE, NOTIFICATION_ID_BASE + NOTIFICATION_ID_RANGE) are ours. */
        private const val NOTIFICATION_ID_BASE = 2000
        private const val NOTIFICATION_ID_RANGE = 1000

        /** Serializes transfers process-wide so the server only ever runs one transcode for us. */
        private val TRANSCODE_MUTEX = Mutex()

        /** Input-data key carrying the item id to download. */
        const val KEY_ITEM_ID = "item_id"
    }
}
