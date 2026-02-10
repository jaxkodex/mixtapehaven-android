package pe.net.libre.mixtapehaven.data.model

import pe.net.libre.mixtapehaven.data.api.BaseItemDto

data class SongWithSize(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: String?,
    val duration: String,
    val fileSize: Long,
    val bitrate: Int?,
    val albumCoverUrl: String?
)

data class PlaylistWithSongs(
    val playlistId: String,
    val name: String,
    val songs: List<SongWithSize>,
    val totalSize: Long,
    val coverUrl: String?
)

fun BaseItemDto.toSongWithSize(serverUrl: String): SongWithSize? {
    if (type != "Audio") return null

    val mediaSource = mediaSources?.firstOrNull()
    val size = mediaSource?.size ?: 0L
    val bitrate = mediaSource?.bitrate

    val artist = albumArtists?.firstOrNull()?.name
        ?: artists?.firstOrNull()
        ?: albumArtist
        ?: "Unknown Artist"

    val coverUrl = imageTags?.get("Primary")?.let { tag ->
        "${serverUrl}/Items/$id/Images/Primary?tag=$tag"
    }

    return SongWithSize(
        id = id,
        title = name,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = formatDuration(runTimeTicks),
        fileSize = size,
        bitrate = bitrate,
        albumCoverUrl = coverUrl
    )
}

private fun formatDuration(ticks: Long?): String {
    if (ticks == null) return "0:00"
    val seconds = ticks / 10_000_000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}
