package pe.net.libre.mixtapehaven.data.repository

import pe.net.libre.mixtapehaven.data.local.entity.DownloadedSongEntity
import pe.net.libre.mixtapehaven.ui.home.Song

/**
 * Mapper object for converting DownloadedSongEntity to Song domain model.
 * Extracted from DownloadsViewModel to enable reuse across ViewModels.
 */
object DownloadedSongMapper {

    /**
     * Convert a single DownloadedSongEntity to Song
     */
    fun toSong(entity: DownloadedSongEntity): Song {
        return Song(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            duration = entity.duration,
            albumCoverUrl = entity.imagePath,
            albumCoverPlaceholder = "",
            isDownloaded = true
        )
    }

    /**
     * Convert a list of DownloadedSongEntity to List<Song>
     */
    fun toSongList(entities: List<DownloadedSongEntity>): List<Song> {
        return entities.map { toSong(it) }
    }
}
