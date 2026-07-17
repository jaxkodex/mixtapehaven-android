package pe.net.libre.mixtapehaven.model

import androidx.compose.ui.graphics.Color

/** What a [VideoItem] represents in the library hierarchy. */
enum class VideoKind { MOVIE, SERIES, EPISODE }

/**
 * A video library item (movie, TV series, or episode). [posterUrl] is the portrait primary image
 * ([backdropUrl] the wide art); [artColor] is the tint fallback when no artwork exists.
 * [resumePositionMs] > 0 means the server has an in-progress watch position for this user.
 */
data class VideoItem(
    val id: String,
    val title: String,
    val kind: VideoKind,
    val yearLabel: String = "",
    val runtimeLabel: String = "",
    val overview: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val artColor: Color = Color(0xFF6C8A7A),
    val runtimeMs: Long = 0L,
    val resumePositionMs: Long = 0L,
    val seriesName: String? = null,
    val seasonEpisodeLabel: String? = null,
)
