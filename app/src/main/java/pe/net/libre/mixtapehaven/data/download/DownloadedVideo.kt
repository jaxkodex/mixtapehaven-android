package pe.net.libre.mixtapehaven.data.download

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/**
 * A movie or episode saved to local storage as a quality-capped transcode for offline playback.
 * [kind] is the [VideoKind] name, [qualityLabel] the cap it was transcoded at (e.g. "720p").
 * [complete] is false while bytes are still being written and true once the file is fully saved.
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
