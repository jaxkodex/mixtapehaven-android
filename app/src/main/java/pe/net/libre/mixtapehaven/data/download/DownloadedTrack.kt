package pe.net.libre.mixtapehaven.data.download

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import pe.net.libre.mixtapehaven.model.Track

/**
 * A track whose original file bytes have been saved to local storage for offline playback.
 * [artColorArgb] is the packed ARGB of the vinyl-tint fallback. [complete] is false while the
 * bytes are still being written (a partial download) and true once the file is fully saved.
 */
@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String,
    val imageUrl: String?,
    val artColorArgb: Int,
    val filePath: String,
    val sizeBytes: Long,
    val complete: Boolean,
)

/** Map a persisted download back to a UI [Track], flagged as downloaded with a human-readable size. */
internal fun DownloadedTrack.toTrack(): Track = Track(
    title = title,
    artist = artist,
    durationLabel = durationLabel,
    artColor = Color(artColorArgb),
    downloaded = true,
    sizeLabel = formatBytes(sizeBytes),
    id = id,
    imageUrl = imageUrl,
)
