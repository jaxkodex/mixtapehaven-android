package pe.net.libre.mixtapehaven.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import pe.net.libre.mixtapehaven.R

/**
 * Minimal dataSync foreground service held open while video downloads run. Without it Android
 * freezes the app process shortly after the screen turns off, aborting the sockets of these
 * multi-minute transfers ("Software caused connection abort"). The service carries no logic of
 * its own: [VideoDownloadManager] starts it with the first active download and stops it with the
 * last, and the mandatory notification is the user's cue that a download is still running.
 */
class VideoDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Video downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading video")
            .setContentText("Saving for offline playback")
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID = 20

        /** Promote the app to a foreground data-sync service while downloads are active. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, VideoDownloadService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VideoDownloadService::class.java))
        }
    }
}
