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
    val imageUrl: String? = null
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val albumCoverUrl: String? = null,
    val albumCoverPlaceholder: String = ""
)

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
        name = "Daft Punk"
    ),
    Artist(
        id = "2",
        name = "Frank Ocean"
    ),
    Artist(
        id = "3",
        name = "Kendrick Lamar"
    ),
    Artist(
        id = "4",
        name = "Tame Impala"
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
