package pe.net.libre.mixtapehaven.ui.home

/**
 * Mock data models and sample data for the home screen
 */

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val coverPlaceholder: String = "" // Placeholder text for albums without images
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val albumCount: Int = 0,
    val songCount: Int = 0
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val albumCoverUrl: String? = null,
    val albumCoverPlaceholder: String = ""
) {
    /**
     * Construct the streaming URL for Jellyfin
     * Note: Authentication is handled via HTTP headers (X-Emby-Token)
     * in PlaybackManager's OkHttp interceptor
     *
     * Using /Audio/{id}/stream with static=true for direct streaming without transcoding.
     * This is the proper endpoint for audio streaming in Jellyfin clients.
     *
     * Parameters:
     * - static=true: Stream the original file without transcoding
     * - mediaSourceId={id}: Specifies which media source to use
     * - api_key can be in query OR headers (we use headers via OkHttp interceptor)
     */
    fun getStreamUrl(serverUrl: String): String {
        return "$serverUrl/Audio/$id/stream?static=true&mediaSourceId=$id"
    }
}

/**
 * Mock data for recently added albums
 */
val mockRecentlyAddedAlbums = listOf(
    Album(
        id = "1",
        title = "Currents",
        artist = "Tame Impala",
        coverPlaceholder = "🌅"
    ),
    Album(
        id = "2",
        title = "Discovery",
        artist = "Daft Punk",
        coverPlaceholder = "🤖"
    ),
    Album(
        id = "3",
        title = "AM",
        artist = "Arctic Monkeys",
        coverPlaceholder = "🎸"
    ),
    Album(
        id = "4",
        title = "Random Access Memories",
        artist = "Daft Punk",
        coverPlaceholder = "💿"
    )
)

/**
 * Mock data for top artists
 */
val mockTopArtists = listOf(
    Artist(
        id = "1",
        name = "Daft Punk",
        albumCount = 8,
        songCount = 96
    ),
    Artist(
        id = "2",
        name = "Frank Ocean",
        albumCount = 3,
        songCount = 45
    ),
    Artist(
        id = "3",
        name = "Kendrick Lamar",
        albumCount = 5,
        songCount = 78
    ),
    Artist(
        id = "4",
        name = "Tame Impala",
        albumCount = 4,
        songCount = 52
    )
)

/**
 * Mock data for all artists (expanded list for detail view)
 */
val mockAllArtists = listOf(
    Artist(
        id = "1",
        name = "Aetheric Echoes",
        albumCount = 12,
        songCount = 142
    ),
    Artist(
        id = "2",
        name = "Arcade Velocity",
        albumCount = 8,
        songCount = 96
    ),
    Artist(
        id = "3",
        name = "Chromatic Wave",
        albumCount = 5,
        songCount = 60
    ),
    Artist(
        id = "4",
        name = "Daft Punk",
        albumCount = 8,
        songCount = 96
    ),
    Artist(
        id = "5",
        name = "Digital Dreamscape",
        albumCount = 15,
        songCount = 180
    ),
    Artist(
        id = "6",
        name = "Frank Ocean",
        albumCount = 3,
        songCount = 45
    ),
    Artist(
        id = "7",
        name = "Glitch Mob",
        albumCount = 4,
        songCount = 55
    ),
    Artist(
        id = "8",
        name = "Kendrick Lamar",
        albumCount = 5,
        songCount = 78
    ),
    Artist(
        id = "9",
        name = "Neon Nomad",
        albumCount = 7,
        songCount = 84
    ),
    Artist(
        id = "10",
        name = "Pink Floyd",
        albumCount = 15,
        songCount = 200
    ),
    Artist(
        id = "11",
        name = "Synthwave Specter",
        albumCount = 22,
        songCount = 250
    ),
    Artist(
        id = "12",
        name = "Tame Impala",
        albumCount = 4,
        songCount = 52
    )
)

/**
 * Mock data for popular songs
 */
val mockPopularSongs = listOf(
    Song(
        id = "1",
        title = "Nikes",
        artist = "Frank Ocean",
        duration = "5:14",
        albumCoverPlaceholder = "🌊"
    ),
    Song(
        id = "2",
        title = "Money",
        artist = "Pink Floyd",
        duration = "6:22",
        albumCoverPlaceholder = "💵"
    ),
    Song(
        id = "3",
        title = "Get Lucky",
        artist = "Daft Punk",
        duration = "6:09",
        albumCoverPlaceholder = "🎵"
    ),
    Song(
        id = "4",
        title = "King Kunta",
        artist = "Kendrick Lamar",
        duration = "3:54",
        albumCoverPlaceholder = "👑"
    ),
    Song(
        id = "5",
        title = "The Less I Know The Better",
        artist = "Tame Impala",
        duration = "3:36",
        albumCoverPlaceholder = "🎹"
    )
)

/**
 * Mock now playing song
 */
val mockNowPlayingSong = Song(
    id = "1",
    title = "Nikes",
    artist = "Frank Ocean",
    duration = "5:14",
    albumCoverPlaceholder = "🌊"
)
