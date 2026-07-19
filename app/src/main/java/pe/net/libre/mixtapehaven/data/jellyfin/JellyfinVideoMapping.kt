package pe.net.libre.mixtapehaven.data.jellyfin

import androidx.compose.ui.graphics.Color
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Mapping from Jellyfin DTOs to the video domain model, kept out of [JellyfinRepository] for size. */

internal const val IMAGE_MAX_WIDTH = 600
internal const val BACKDROP_MAX_WIDTH = 1280
internal const val TICKS_PER_MS = 10_000L

/** Jellyfin URLs carry no auth by default; media/image requests authenticate via this query param. */
internal fun String.withApiKey(client: ApiClient): String {
    val token = client.accessToken ?: return this
    val separator = if (contains('?')) '&' else '?'
    return "$this${separator}ApiKey=$token"
}

internal fun BaseItemDto.toVideoItem(client: ApiClient): VideoItem {
    val runtimeMs = (runTimeTicks ?: 0L) / TICKS_PER_MS
    return VideoItem(
        id = id.toString(),
        title = name ?: "Untitled",
        kind = when (type) {
            BaseItemKind.SERIES -> VideoKind.SERIES
            BaseItemKind.EPISODE -> VideoKind.EPISODE
            else -> VideoKind.MOVIE
        },
        yearLabel = productionYear?.toString().orEmpty(),
        runtimeLabel = formatRuntime(runtimeMs),
        overview = overview.orEmpty(),
        posterUrl = videoPrimaryImageUrl(client),
        backdropUrl = backdropImageUrl(client),
        artColor = videoColorFor(id.toString()),
        runtimeMs = runtimeMs,
        resumePositionMs = (userData?.playbackPositionTicks ?: 0L) / TICKS_PER_MS,
        lastPlayedAtMs = userData?.lastPlayedDate?.toEpochMilliUtc() ?: 0L,
        seriesName = seriesName,
        seriesId = seriesId?.toString(),
        seasonEpisodeLabel = if (type == BaseItemKind.EPISODE && parentIndexNumber != null && indexNumber != null) {
            "S$parentIndexNumber E$indexNumber"
        } else {
            null
        },
        played = userData?.played == true,
        unplayedCount = userData?.unplayedItemCount ?: 0,
        genres = genres.orEmpty(),
        communityRating = communityRating,
        officialRating = officialRating.orEmpty(),
        seasonNumber = if (type == BaseItemKind.EPISODE) parentIndexNumber else null,
    )
}

/**
 * "Season 2" for a grouped episode list, from the number the episodes carry. Episodes with no
 * season number (specials, or a flat library) land under [SPECIALS_LABEL] rather than "Season 0",
 * which reads as a real season the user does not have.
 */
internal fun seasonLabel(seasonNumber: Int?): String = when {
    seasonNumber == null -> SPECIALS_LABEL
    seasonNumber <= 0 -> SPECIALS_LABEL
    else -> "Season $seasonNumber"
}

internal const val SPECIALS_LABEL = "Specials"

/**
 * Episodes grouped into seasons in ascending season order, with [SPECIALS_LABEL] last.
 *
 * Season 0 is Jellyfin's specials bucket and sorts *before* season 1 numerically, so it is pushed
 * to the end explicitly — a series should open on its first real season, not its specials.
 */
internal fun groupBySeason(episodes: List<VideoItem>): List<Pair<String, List<VideoItem>>> =
    episodes
        .groupBy { it.seasonNumber ?: 0 }
        .toList()
        .sortedBy { (season, _) -> if (season <= 0) Int.MAX_VALUE else season }
        .map { (season, items) -> seasonLabel(season) to items }

private fun BaseItemDto.videoPrimaryImageUrl(client: ApiClient): String? {
    val tag = imageTags?.get(ImageType.PRIMARY) ?: return null
    return client.imageApi
        .getItemImageUrl(id, ImageType.PRIMARY, tag = tag, maxWidth = IMAGE_MAX_WIDTH)
        .withApiKey(client)
}

/** Wide art for 16:9 surfaces: a real backdrop when present, else the THUMB some libraries use. */
private fun BaseItemDto.backdropImageUrl(client: ApiClient): String? {
    val backdrop = backdropImageTags?.firstOrNull()?.let { ImageType.BACKDROP to it }
    val thumb = imageTags?.get(ImageType.THUMB)?.let { ImageType.THUMB to it }
    val (type, tag) = backdrop ?: thumb ?: return null
    return client.imageApi
        .getItemImageUrl(id, type, tag = tag, maxWidth = BACKDROP_MAX_WIDTH)
        .withApiKey(client)
}

/** Jellyfin serialises timestamps as zoneless UTC, so they are read back against [ZoneOffset.UTC]. */
private fun LocalDateTime.toEpochMilliUtc(): Long = toInstant(ZoneOffset.UTC).toEpochMilli()

internal fun formatRuntime(runtimeMs: Long): String {
    if (runtimeMs <= 0) return ""
    val totalMinutes = runtimeMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/**
 * Remaining watch time for the Continue watching card ("23 min left", "1h 12m left"). Empty when
 * the runtime is unknown or the item is effectively finished, so the caller can drop the segment.
 */
internal fun formatTimeLeft(runtimeMs: Long, positionMs: Long): String {
    // An unknown runtime yields no remaining time at all, rather than a bogus countdown from 0.
    val remainingMs = if (runtimeMs <= 0L) 0L else runtimeMs - positionMs
    if (remainingMs < 60_000) return ""
    val totalMinutes = remainingMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m left" else "$totalMinutes min left"
}

private val VIDEO_PALETTE = listOf(
    Color(0xFFC65B4E),
    Color(0xFF9A8A3C),
    Color(0xFFA86B7E),
    Color(0xFFB5633B),
    Color(0xFFB59A4E),
    Color(0xFF6C8A7A),
)

internal fun videoColorFor(key: String): Color =
    VIDEO_PALETTE[(key.hashCode() and Int.MAX_VALUE) % VIDEO_PALETTE.size]
