package pe.net.libre.mixtapehaven.data.repository

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.first
import pe.net.libre.mixtapehaven.data.api.BaseItemDto
import pe.net.libre.mixtapehaven.data.api.JellyfinApiClient
import pe.net.libre.mixtapehaven.data.api.JellyfinApiService
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.ui.home.Album
import pe.net.libre.mixtapehaven.ui.home.Artist
import pe.net.libre.mixtapehaven.ui.home.Playlist
import pe.net.libre.mixtapehaven.ui.home.Song

class MediaRepository(
    private val dataStoreManager: DataStoreManager,
    private val context: Context
) {
    private var apiService: JellyfinApiService? = null
    private var serverUrl: String? = null
    private var accessToken: String? = null

    /**
     * Initialize the API service with stored credentials
     */
    private suspend fun ensureApiService(): JellyfinApiService {
        val currentServerUrl = dataStoreManager.serverUrl.first()
        val currentAccessToken = dataStoreManager.accessToken.first()

        if (apiService == null ||
            serverUrl != currentServerUrl ||
            accessToken != currentAccessToken) {

            serverUrl = currentServerUrl
            accessToken = currentAccessToken

            if (serverUrl.isNullOrBlank()) {
                throw IllegalStateException("No server URL configured")
            }
            if (accessToken.isNullOrBlank()) {
                throw IllegalStateException("No access token found. Please login again.")
            }

            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            apiService = JellyfinApiClient.createAuthenticatedService(
                baseUrl = serverUrl!!,
                accessToken = accessToken!!,
                deviceId = deviceId
            )
        }
        return apiService!!
    }

    /**
     * Get recently added albums
     */
    suspend fun getRecentlyAddedAlbums(limit: Int = 10): Result<List<Album>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val items = service.getLatestMedia(
                userId = userId,
                includeItemTypes = "MusicAlbum",
                limit = limit,
                fields = "PrimaryImageAspectRatio,SortName,Path,ChildCount"
            )

            val albums = items.map { item -> mapToAlbum(item) }
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all albums
     */
    suspend fun getAllAlbums(limit: Int = 50, startIndex: Int = 0): Result<List<Album>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "MusicAlbum",
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = startIndex,
                fields = "PrimaryImageAspectRatio,SortName,Path,ChildCount"
            )

            val albums = response.items.map { item -> mapToAlbum(item) }
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get top artists
     */
    suspend fun getTopArtists(limit: Int = 10): Result<List<Artist>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "MusicArtist",
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = limit,
                fields = "PrimaryImageAspectRatio,SortName,ChildCount"
            )

            val artists = response.items.map { item -> mapToArtist(item) }
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all artists
     */
    suspend fun getAllArtists(limit: Int = 50, startIndex: Int = 0): Result<List<Artist>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "MusicArtist",
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = startIndex,
                fields = "PrimaryImageAspectRatio,SortName,ChildCount"
            )

            val artists = response.items.map { item -> mapToArtist(item) }
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get popular songs
     */
    suspend fun getPopularSongs(limit: Int = 20): Result<List<Song>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "Audio",
                recursive = true,
                sortBy = "PlayCount,DatePlayed",
                sortOrder = "Descending",
                limit = limit,
                fields = "PrimaryImageAspectRatio,Path,MediaSources"
            )

            val songs = response.items.map { item -> mapToSong(item) }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all songs
     */
    suspend fun getAllSongs(limit: Int = 50, startIndex: Int = 0): Result<List<Song>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "Audio",
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = startIndex,
                fields = "PrimaryImageAspectRatio,Path,MediaSources"
            )

            val songs = response.items.map { item -> mapToSong(item) }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user playlists
     */
    suspend fun getUserPlaylists(limit: Int = 10): Result<List<Playlist>> {
        return try {
            val service = ensureApiService()
            val userId = dataStoreManager.userId.first()
                ?: throw IllegalStateException("No user ID found")

            val response = service.getItems(
                userId = userId,
                includeItemTypes = "Playlist",
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = limit,
                fields = "PrimaryImageAspectRatio,ChildCount"
            )

            val playlists = response.items.map { item -> mapToPlaylist(item) }
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Map BaseItemDto to Album
     */
    private fun mapToAlbum(item: BaseItemDto): Album {
        val artistName = item.albumArtist
            ?: item.albumArtists?.firstOrNull()?.name
            ?: item.artists?.firstOrNull()
            ?: "Unknown Artist"

        return Album(
            id = item.id,
            title = item.name,
            artist = artistName,
            coverUrl = getImageUrl(item.id, "Primary", item.imageTags?.get("Primary")),
            coverPlaceholder = getPlaceholderEmoji(item.name)
        )
    }

    /**
     * Map BaseItemDto to Artist
     */
    private fun mapToArtist(item: BaseItemDto): Artist {
        return Artist(
            id = item.id,
            name = item.name,
            imageUrl = getImageUrl(item.id, "Primary", item.imageTags?.get("Primary")),
            albumCount = item.childCount ?: 0,
            songCount = 0 // Will need separate call to get song count
        )
    }

    /**
     * Map BaseItemDto to Song
     */
    private fun mapToSong(item: BaseItemDto): Song {
        val artistName = item.artists?.firstOrNull()
            ?: item.artistItems?.firstOrNull()?.name
            ?: item.albumArtist
            ?: "Unknown Artist"

        return Song(
            id = item.id,
            title = item.name,
            artist = artistName,
            duration = formatDuration(item.runTimeTicks),
            albumCoverUrl = getImageUrl(
                item.albumId ?: item.id,
                "Primary",
                item.imageTags?.get("Primary")
            ),
            albumCoverPlaceholder = getPlaceholderEmoji(item.album ?: item.name)
        )
    }

    /**
     * Map BaseItemDto to Playlist
     */
    private fun mapToPlaylist(item: BaseItemDto): Playlist {
        return Playlist(
            id = item.id,
            name = item.name,
            songCount = item.childCount ?: 0,
            coverUrl = getImageUrl(item.id, "Primary", item.imageTags?.get("Primary")),
            coverPlaceholder = getPlaceholderEmoji(item.name)
        )
    }

    /**
     * Generate Jellyfin image URL
     */
    private fun getImageUrl(itemId: String, imageType: String, tag: String?): String? {
        if (tag == null || serverUrl == null) return null
        return "${serverUrl}/Items/${itemId}/Images/${imageType}?tag=${tag}"
    }

    /**
     * Convert runtime ticks to duration string (MM:SS)
     */
    private fun formatDuration(ticks: Long?): String {
        if (ticks == null) return "0:00"
        val seconds = (ticks / 10_000_000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    /**
     * Generate placeholder emoji based on name
     */
    private fun getPlaceholderEmoji(name: String): String {
        val emojis = listOf("🎵", "🎶", "🎸", "🎹", "🎤", "🎧", "🎼", "🎺", "🎷", "🥁", "💿", "📻", "🎭", "🌟", "✨")
        return emojis[name.hashCode().mod(emojis.size)]
    }
}
