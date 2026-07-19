package pe.net.libre.mixtapehaven.model

import androidx.compose.ui.graphics.Color

/** What a [VideoItem] represents in the library hierarchy. */
enum class VideoKind { MOVIE, SERIES, EPISODE }

/**
 * A video library item (movie, TV series, or episode). [posterUrl] is the PRIMARY image — a
 * portrait 2:3 poster for movies/series, but a 16:9 still for episodes (that is what Jellyfin
 * stores as an episode's primary). [backdropUrl] is the wide art; [artColor] is the tint fallback
 * when no artwork exists. [resumePositionMs] > 0 means the server has an in-progress watch
 * position for this user, last updated at [lastPlayedAtMs] (epoch millis, 0 when never played).
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
    val lastPlayedAtMs: Long = 0L,
    val seriesName: String? = null,
    /** Parent series of an episode, for Next Up and autoplay lookups. Null for movies. */
    val seriesId: String? = null,
    val seasonEpisodeLabel: String? = null,
) {
    /** Fraction of the runtime already watched, in 0f..1f. 0 when the runtime is unknown. */
    val progressFraction: Float
        get() = if (runtimeMs <= 0L) 0f else (resumePositionMs.toFloat() / runtimeMs).coerceIn(0f, 1f)
}
