package pe.net.libre.mixtapehaven.data.download

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/**
 * Lifecycle of a video download row. Stored as the enum name in [DownloadedVideo.status];
 * unknown values (from a future schema) read back as [FAILED] so the UI offers a retry.
 */
enum class VideoDownloadStatus {
    /** Enqueued with WorkManager, waiting for its turn or for its network constraint. */
    QUEUED,

    /** A worker is streaming bytes right now. */
    RUNNING,

    /** Gave up after retries (or hit a permanent error); the row offers a manual retry. */
    FAILED,

    /** Fully saved; [DownloadedVideo.filePath] points at the playable file. */
    COMPLETE,
    ;

    companion object {
        fun fromName(name: String): VideoDownloadStatus = entries.firstOrNull { it.name == name } ?: FAILED
    }
}

/**
 * A movie or episode saved to local storage as a quality-capped transcode for offline playback.
 * [kind] is the [VideoKind] name, [qualityLabel] the cap it was transcoded at (e.g. "720p").
 * [complete] is false while bytes are still being written and true once the file is fully saved;
 * [status] carries the finer-grained [VideoDownloadStatus] name for queue/retry UI.
 */
@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val seriesName: String?,
    val seasonEpisodeLabel: String?,
    val runtimeLabel: String,
    val posterUrl: String?,
    val artColorArgb: Int,
    val qualityLabel: String,
    val filePath: String,
    val sizeBytes: Long,
    val complete: Boolean,
    val status: String,
)

/** Map a persisted video download back to a UI [VideoItem]. */
internal fun DownloadedVideo.toVideoItem(): VideoItem = VideoItem(
    id = id,
    title = title,
    kind = VideoKind.entries.firstOrNull { it.name == kind } ?: VideoKind.MOVIE,
    runtimeLabel = runtimeLabel,
    posterUrl = posterUrl,
    artColor = Color(artColorArgb),
    seriesName = seriesName,
    seasonEpisodeLabel = seasonEpisodeLabel,
)
