package pe.net.libre.mixtapehaven.data.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.net.libre.mixtapehaven.data.download.VideoProgress
import pe.net.libre.mixtapehaven.data.download.VideoProgressDao
import pe.net.libre.mixtapehaven.data.download.toProgress
import pe.net.libre.mixtapehaven.data.download.toVideoItem
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.jellyfin.VideoPlaybackEvent
import pe.net.libre.mixtapehaven.model.VideoItem

/**
 * Watch positions for video, mirrored locally so they survive with no server.
 *
 * Jellyfin owns the canonical resume point, but a downloaded title is precisely the one played
 * offline: reporting would silently no-op and the next launch would restart from zero. Every
 * position therefore lands in the local table as well, and [resolveResumePosition] arbitrates by
 * recency when the two disagree.
 */
class VideoProgressStore(
    private val repository: JellyfinRepository,
    private val dao: VideoProgressDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Locally known in-progress titles, most recent first — the offline Continue watching source. */
    fun observeLocal(limit: Int = CONTINUE_LIMIT): Flow<List<VideoItem>> =
        dao.observeContinueWatching(limit).map { rows -> rows.map { it.toVideoItem() } }

    /**
     * The item to play for [itemId] and the position to start it at, merging the server's copy
     * (authoritative metadata, may be unreachable) with the local row (always available).
     */
    suspend fun resolvePlayback(itemId: String): ResolvedPlayback {
        val local = dao.findById(itemId)
        val remote = runCatching { repository.videoItem(itemId) }.getOrNull()
        return ResolvedPlayback(
            item = remote ?: local?.toVideoItem(),
            positionMs = resolveResumePosition(local, remote),
        )
    }

    /**
     * Record [positionMs] for [item] both locally and (best-effort) on the server.
     *
     * [runtimeMs] comes from the player rather than the metadata because a transcode's reported
     * duration is what the position is actually measured against.
     */
    suspend fun record(
        item: VideoItem,
        positionMs: Long,
        runtimeMs: Long,
        event: VideoPlaybackEvent,
        paused: Boolean = false,
        transcoding: Boolean = false,
    ) {
        if (isFinished(positionMs, runtimeMs)) {
            // Finished: drop it from Continue watching instead of stranding it at 99%.
            dao.deleteById(item.id)
        } else {
            dao.upsert(item.toProgress(positionMs, runtimeMs, now()))
        }
        repository.reportVideoPlayback(item.id, positionMs, event, paused = paused, transcoding = transcoding)
    }

    /** Forget the local position for [itemId] (e.g. when its download is deleted). */
    suspend fun clear(itemId: String) = dao.deleteById(itemId)

    suspend fun clearAll() = dao.clear()

    private companion object {
        const val CONTINUE_LIMIT = 12
    }
}

/** The item to play and where to start it, as resolved by [VideoProgressStore.resolvePlayback]. */
data class ResolvedPlayback(val item: VideoItem?, val positionMs: Long)

/**
 * How much of the runtime must be watched before a title counts as finished. Matches Jellyfin's
 * own threshold, so a title the server has already dropped from Continue Watching does not linger
 * in ours.
 */
private const val FINISHED_FRACTION = 0.95f

/** True once [positionMs] is far enough into [runtimeMs] that the title counts as watched. */
internal fun isFinished(positionMs: Long, runtimeMs: Long): Boolean =
    runtimeMs > 0L && positionMs >= runtimeMs * FINISHED_FRACTION

/**
 * The position to resume from given the [local] and [remote] records: whichever was written more
 * recently wins.
 *
 * Taking the larger position instead would be wrong — a user who deliberately rewound on one
 * device would be dragged forward again by the other side's stale, larger position.
 */
internal fun resolveResumePosition(local: VideoProgress?, remote: VideoItem?): Long = when {
    local == null -> remote?.resumePositionMs ?: 0L
    remote == null -> local.positionMs
    remote.lastPlayedAtMs > local.updatedAtMs -> remote.resumePositionMs
    else -> local.positionMs
}
