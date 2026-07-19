package pe.net.libre.mixtapehaven.data.download

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/**
 * A locally recorded watch position for one movie or episode.
 *
 * The server owns the canonical resume point, but it is unreachable offline — and a downloaded
 * title is exactly the one you watch offline. Every position update is therefore written here too,
 * stamped with [updatedAtMs], and the fresher of the two wins on resume (see
 * [pe.net.libre.mixtapehaven.data.playback.resolveResumePosition]).
 *
 * The display fields are denormalised so the Continue watching rail can render with no network at
 * all; they are refreshed on every write from whatever the player already knows about the item.
 */
@Entity(tableName = "video_progress")
data class VideoProgress(
    @PrimaryKey val id: String,
    val positionMs: Long,
    val runtimeMs: Long,
    val updatedAtMs: Long,
    val title: String,
    val kind: String,
    val seriesName: String?,
    val seasonEpisodeLabel: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val artColorArgb: Int,
)

/** Map a local progress row back to a UI [VideoItem]. */
internal fun VideoProgress.toVideoItem(): VideoItem = VideoItem(
    id = id,
    title = title,
    kind = VideoKind.entries.firstOrNull { it.name == kind } ?: VideoKind.MOVIE,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    artColor = Color(artColorArgb),
    runtimeMs = runtimeMs,
    resumePositionMs = positionMs,
    lastPlayedAtMs = updatedAtMs,
    seriesName = seriesName,
    seasonEpisodeLabel = seasonEpisodeLabel,
)

/** Build a progress row for [item] at [positionMs], stamped [nowMs]. */
internal fun VideoItem.toProgress(positionMs: Long, runtimeMs: Long, nowMs: Long) = VideoProgress(
    id = id,
    positionMs = positionMs,
    runtimeMs = if (runtimeMs > 0L) runtimeMs else this.runtimeMs,
    updatedAtMs = nowMs,
    title = title,
    kind = kind.name,
    seriesName = seriesName,
    seasonEpisodeLabel = seasonEpisodeLabel,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    artColorArgb = artColor.toArgb(),
)

/** Room access to locally recorded video watch positions. */
@Dao
interface VideoProgressDao {

    /** In-progress titles, most recently watched first, for the Continue watching rail. */
    @Query("SELECT * FROM video_progress WHERE positionMs > 0 ORDER BY updatedAtMs DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int): Flow<List<VideoProgress>>

    @Query("SELECT * FROM video_progress WHERE id = :id")
    suspend fun findById(id: String): VideoProgress?

    @Upsert
    suspend fun upsert(progress: VideoProgress)

    @Query("DELETE FROM video_progress WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM video_progress")
    suspend fun clear()
}
