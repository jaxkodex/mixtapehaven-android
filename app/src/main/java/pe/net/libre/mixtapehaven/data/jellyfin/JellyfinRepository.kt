package pe.net.libre.mixtapehaven.data.jellyfin

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import pe.net.libre.mixtapehaven.data.session.Session
import pe.net.libre.mixtapehaven.data.session.SessionStore
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import java.util.UUID

/** Wraps the Jellyfin Kotlin SDK: authentication, library browsing, search, and stream/image URLs. */
class JellyfinRepository(
    private val jellyfin: Jellyfin,
    private val sessionStore: SessionStore,
) {
    @Volatile
    private var api: ApiClient? = null

    @Volatile
    private var userId: UUID? = null

    val session: Flow<Session?> = sessionStore.session

    /** Rebuild the API client from a persisted session on app start. No-op if not logged in. */
    suspend fun restore() {
        val saved = sessionStore.session.first() ?: return
        api = jellyfin.createApi(baseUrl = saved.serverUrl, accessToken = saved.accessToken)
        userId = runCatching { UUID.fromString(saved.userId) }.getOrNull()
    }

    /** Authenticate against [serverUrl] and persist the session on success. */
    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<Unit> =
        runCatching {
            val baseUrl = serverUrl.trim().trimEnd('/').let {
                if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
            }
            val client = jellyfin.createApi(baseUrl = baseUrl)
            val result by client.userApi.authenticateUserByName(username = username, password = password)
            val token = requireNotNull(result.accessToken) { "Server returned no access token" }
            val user = requireNotNull(result.user) { "Server returned no user" }
            client.update(accessToken = token)
            api = client
            userId = user.id
            sessionStore.save(
                Session(
                    serverUrl = baseUrl,
                    accessToken = token,
                    userId = user.id.toString(),
                    userName = user.name ?: username,
                ),
            )
        }

    suspend fun signOut() {
        sessionStore.clear()
        api = null
        userId = null
    }

    suspend fun recentlyAddedAlbums(limit: Int = 40): List<Album> {
        val client = requireApi()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
                recursive = true,
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit,
            ),
        )
        return result.items.orEmpty().map { it.toAlbum(client) }
    }

    suspend fun search(query: String, limit: Int = 50): List<Track> {
        val client = requireApi()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                recursive = true,
                searchTerm = query,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                limit = limit,
            ),
        )
        return result.items.orEmpty().map { it.toTrack(client) }
    }

    /** A randomly ordered batch of tracks from the whole library, for the endless Random Walk queue. */
    suspend fun randomTracks(limit: Int = 50): List<Track> {
        val client = requireApi()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                recursive = true,
                sortBy = listOf(ItemSortBy.RANDOM),
                limit = limit,
            ),
        )
        return result.items.orEmpty().map { it.toTrack(client) }
    }

    suspend fun albumTracks(albumId: String): List<Track> {
        val client = requireApi()
        val parent = runCatching { UUID.fromString(albumId) }.getOrNull() ?: return emptyList()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                parentId = parent,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER),
                recursive = true,
            ),
        )
        return result.items.orEmpty().map { it.toTrack(client) }
    }

    /** Direct stream URL for [trackId]: the original file bytes (no transcoding), for local-first playback. */
    fun audioStreamUrl(trackId: String): String? {
        val client = api ?: return null
        return runCatching { UUID.fromString(trackId) }.getOrNull()?.let { id ->
            client.audioApi
                .getAudioStreamUrl(itemId = id, static = true)
                .withApiKey(client)
        }
    }

    private fun requireApi(): ApiClient =
        api ?: error("Not authenticated with a Jellyfin server")

    private fun BaseItemDto.toAlbum(client: ApiClient): Album = Album(
        id = id.toString(),
        title = name ?: "Unknown album",
        artist = albumArtist ?: artists?.firstOrNull() ?: "Unknown artist",
        artColor = colorFor(id.toString()),
        imageUrl = primaryImageUrl(client),
        downloaded = false,
    )

    private fun BaseItemDto.toTrack(client: ApiClient): Track = Track(
        id = id.toString(),
        title = name ?: "Unknown track",
        artist = artists?.joinToString(", ").orEmpty().ifEmpty { albumArtist ?: "Unknown artist" },
        durationLabel = formatDuration(runTimeTicks),
        artColor = colorFor(id.toString()),
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

    private fun String.withApiKey(client: ApiClient): String {
        val token = client.accessToken ?: return this
        val separator = if (contains('?')) '&' else '?'
        return "$this${separator}ApiKey=$token"
    }

    private companion object {
        const val IMAGE_MAX_WIDTH = 600
        const val TICKS_PER_SECOND = 10_000_000L

        val PALETTE = listOf(
            Color(0xFFC65B4E),
            Color(0xFF9A8A3C),
            Color(0xFFA86B7E),
            Color(0xFFB5633B),
            Color(0xFFB59A4E),
            Color(0xFF6C8A7A),
        )

        fun colorFor(key: String): Color =
            PALETTE[(key.hashCode() and Int.MAX_VALUE) % PALETTE.size]

        fun formatDuration(ticks: Long?): String {
            if (ticks == null || ticks <= 0) return "--:--"
            val totalSeconds = ticks / TICKS_PER_SECOND
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
