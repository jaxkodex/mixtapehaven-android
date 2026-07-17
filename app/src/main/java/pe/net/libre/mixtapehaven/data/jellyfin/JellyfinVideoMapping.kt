package pe.net.libre.mixtapehaven.data.jellyfin

import androidx.compose.ui.graphics.Color
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

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
        seriesName = seriesName,
        seasonEpisodeLabel = if (type == BaseItemKind.EPISODE && parentIndexNumber != null && indexNumber != null) {
            "S$parentIndexNumber E$indexNumber"
        } else {
            null
        },
    )
}

private fun BaseItemDto.videoPrimaryImageUrl(client: ApiClient): String? {
    val tag = imageTags?.get(ImageType.PRIMARY) ?: return null
    return client.imageApi
        .getItemImageUrl(id, ImageType.PRIMARY, tag = tag, maxWidth = IMAGE_MAX_WIDTH)
        .withApiKey(client)
}

private fun BaseItemDto.backdropImageUrl(client: ApiClient): String? {
    val tag = backdropImageTags?.firstOrNull() ?: return null
    return client.imageApi
        .getItemImageUrl(id, ImageType.BACKDROP, tag = tag, maxWidth = BACKDROP_MAX_WIDTH)
        .withApiKey(client)
}

internal fun formatRuntime(runtimeMs: Long): String {
    if (runtimeMs <= 0) return ""
    val totalMinutes = runtimeMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
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
