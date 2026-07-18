package pe.net.libre.mixtapehaven.data.jellyfin

import androidx.compose.ui.graphics.Color
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track

/** Mapping from Jellyfin DTOs to the audio domain model, kept out of [JellyfinRepository] for size. */

private const val TICKS_PER_SECOND = 10_000_000L

internal fun BaseItemDto.toAlbum(client: ApiClient): Album = Album(
    id = id.toString(),
    title = name ?: "Unknown album",
    artist = albumArtist ?: artists?.firstOrNull() ?: "Unknown artist",
    artColor = audioColorFor(id.toString()),
    imageUrl = primaryImageUrl(client),
    downloaded = false,
)

internal fun BaseItemDto.toTrack(client: ApiClient): Track = Track(
    id = id.toString(),
    title = name ?: "Unknown track",
    artist = artists?.joinToString(", ").orEmpty().ifEmpty { albumArtist ?: "Unknown artist" },
    durationLabel = formatDuration(runTimeTicks),
    artColor = audioColorFor(id.toString()),
    imageUrl = primaryImageUrl(client),
)

private fun BaseItemDto.primaryImageUrl(client: ApiClient): String? {
    val ownTag = imageTags?.get(ImageType.PRIMARY)
    return when {
        ownTag != null ->
            client.imageApi.getItemImageUrl(id, ImageType.PRIMARY, tag = ownTag, maxWidth = IMAGE_MAX_WIDTH)
                .withApiKey(client)

        albumId != null && albumPrimaryImageTag != null ->
            client.imageApi.getItemImageUrl(
                albumId!!,
                ImageType.PRIMARY,
                tag = albumPrimaryImageTag,
                maxWidth = IMAGE_MAX_WIDTH,
            ).withApiKey(client)

        else -> null
    }
}

private val AUDIO_PALETTE = listOf(
    Color(0xFFC65B4E),
    Color(0xFF9A8A3C),
    Color(0xFFA86B7E),
    Color(0xFFB5633B),
    Color(0xFFB59A4E),
    Color(0xFF6C8A7A),
)

private fun audioColorFor(key: String): Color =
    AUDIO_PALETTE[(key.hashCode() and Int.MAX_VALUE) % AUDIO_PALETTE.size]

private fun formatDuration(ticks: Long?): String {
    if (ticks == null || ticks <= 0) return "--:--"
    val totalSeconds = ticks / TICKS_PER_SECOND
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
