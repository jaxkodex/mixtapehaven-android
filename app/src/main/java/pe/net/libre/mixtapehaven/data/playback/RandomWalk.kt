package pe.net.libre.mixtapehaven.data.playback

import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.model.Track

/**
 * Drives Random Walk: a one-tap, never-ending shuffled queue across the whole library. Seeds the
 * queue with a random batch, then refills it forever via [PlayerController.onQueueRunningLow],
 * skipping tracks played in the recent window so the same song never repeats back-to-back.
 */
class RandomWalk(
    private val repository: JellyfinRepository,
    private val playerController: PlayerController,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    // Ids handed out recently, oldest first; capped at RECENT_WINDOW to avoid starving small libraries.
    private val recentIds = ArrayDeque<String>()

    /** Seed the queue and arm the refill hook. No-op if the library has no tracks. */
    suspend fun start() {
        val initial = nextBatch()
        if (initial.isEmpty()) return
        playerController.setSource(PlaybackSource.RANDOM_WALK)
        playerController.play(initial, startIndex = 0)
        playerController.onQueueRunningLow = { nextBatch() }
    }

    /**
     * Fetch a fresh random batch, drop ids in the recent window, and remember what we hand out.
     * Over-fetches by the recent-window size so a full window of collisions still yields [batchSize].
     */
    private suspend fun nextBatch(): List<Track> {
        val fetched = repository.randomTracks(batchSize + recentIds.size)
        val fresh = filterRecent(fetched, recentIds.toSet()).take(batchSize)
        rememberPlayed(fresh)
        return fresh
    }

    private fun rememberPlayed(tracks: List<Track>) {
        tracks.forEach { track ->
            val id = track.id ?: return@forEach
            recentIds.addLast(id)
            while (recentIds.size > RECENT_WINDOW) recentIds.removeFirst()
        }
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val RECENT_WINDOW = 50
    }
}

/**
 * Drop tracks whose id is in [recentIds] so refilled batches never replay the recent window. Tracks
 * without an id pass through (they are dropped later when no stream URL can be resolved). Pure so the
 * Random Walk dedupe is unit-coverable.
 */
internal fun filterRecent(tracks: List<Track>, recentIds: Set<String>): List<Track> =
    tracks.filter { it.id !in recentIds }
