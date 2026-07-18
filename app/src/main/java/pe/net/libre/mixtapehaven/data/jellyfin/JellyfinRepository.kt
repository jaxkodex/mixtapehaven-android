package pe.net.libre.mixtapehaven.data.jellyfin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.dynamicHlsApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import pe.net.libre.mixtapehaven.data.session.Session
import pe.net.libre.mixtapehaven.data.session.SessionStore
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.model.VideoItem
import java.util.UUID

/** Lifecycle moment of video playback reported to the server via [JellyfinRepository.reportVideoPlayback]. */
enum class VideoPlaybackEvent { STARTED, PROGRESS, STOPPED }

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

    /** Movies and TV series for the Home "Movies & shows" row, newest first. Empty if no video libraries. */
    suspend fun moviesAndShows(limit: Int = 20): List<VideoItem> {
        val client = requireApi()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                recursive = true,
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit,
            ),
        )
        return result.items.orEmpty().map { it.toVideoItem(client) }
    }

    /** Full detail (overview + this user's resume position) for one movie/series/episode. */
    suspend fun videoItem(itemId: String): VideoItem? {
        val client = requireApi()
        val id = runCatching { UUID.fromString(itemId) }.getOrNull() ?: return null
        val item by client.userLibraryApi.getItem(itemId = id, userId = userId)
        return item.toVideoItem(client)
    }

    /** All episodes of [seriesId] in season/episode order, with overviews and resume positions. */
    suspend fun seriesEpisodes(seriesId: String): List<VideoItem> {
        val client = requireApi()
        val id = runCatching { UUID.fromString(seriesId) }.getOrNull() ?: return emptyList()
        val result by client.tvShowsApi.getEpisodes(
            seriesId = id,
            userId = userId,
            fields = listOf(ItemFields.OVERVIEW),
        )
        return result.items.orEmpty().map { it.toVideoItem(client) }
    }

    /**
     * Ordered stream URL candidates for [itemId]: direct play of the original file first, then an
     * HLS transcode pinned to h264/aac for anything the device cannot play natively. The player
     * tries them in order and falls through on error.
     */
    fun videoStreamCandidates(itemId: String): List<String> {
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return emptyList()
        val direct = client.videosApi
            .getVideoStreamUrl(itemId = id, static = true)
            .withApiKey(client)
        val hls = client.dynamicHlsApi
            .getMasterHlsVideoPlaylistUrl(
                itemId = id,
                // The server requires a media source; the default source id is the item id unhyphenated.
                mediaSourceId = itemId.replace("-", ""),
                videoCodec = "h264",
                audioCodec = "aac",
            )
            .withApiKey(client)
        return listOf(direct, hls)
    }

    /**
     * Report a video playback [event] at [positionMs] to the server. Progress feeds the
     * server-side resume point (and Continue Watching); STOPPED finalizes it. Failures are
     * swallowed: playstate reporting must never break playback.
     */
    suspend fun reportVideoPlayback(
        itemId: String,
        positionMs: Long,
        event: VideoPlaybackEvent,
        paused: Boolean = false,
        transcoding: Boolean = false,
    ) {
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return
        val playMethod = if (transcoding) PlayMethod.TRANSCODE else PlayMethod.DIRECT_STREAM
        runCatching {
            when (event) {
                VideoPlaybackEvent.STARTED -> {
                    client.playStateApi.onPlaybackStart(itemId = id, playMethod = playMethod)
                    if (positionMs > 0) {
                        reportVideoPlayback(itemId, positionMs, VideoPlaybackEvent.PROGRESS, transcoding = transcoding)
                    }
                }

                VideoPlaybackEvent.PROGRESS -> client.playStateApi.onPlaybackProgress(
                    itemId = id,
                    positionTicks = positionMs * TICKS_PER_MS,
                    isPaused = paused,
                    playMethod = playMethod,
                )

                VideoPlaybackEvent.STOPPED -> client.playStateApi.onPlaybackStopped(
                    itemId = id,
                    positionTicks = positionMs * TICKS_PER_MS,
                )
            }
        }
    }

    /**
     * URL of a server-side transcode of [itemId] capped at [maxHeight]/[videoBitRate]/[audioBitRate]
     * (h264/aac), for saving a phone-sized offline copy instead of the original file.
     *
     * The container is MPEG-TS, not mp4: ffmpeg muxing mp4 over a non-seekable HTTP response can
     * never backpatch the mdat size, so the moov index at the tail is unreachable and ExoPlayer
     * rejects the saved file as malformed. TS needs no finalization and stays seekable.
     */
    fun videoDownloadUrl(itemId: String, maxHeight: Int, videoBitRate: Int, audioBitRate: Int): String? {
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return null
        return client.videosApi
            .getVideoStreamUrl(
                itemId = id,
                container = "ts",
                static = false,
                // The server requires a media source; the default source id is the item id unhyphenated.
                mediaSourceId = itemId.replace("-", ""),
                videoCodec = "h264",
                audioCodec = "aac",
                maxHeight = maxHeight,
                videoBitRate = videoBitRate,
                audioBitRate = audioBitRate,
                context = EncodingContext.STATIC,
            )
            .withApiKey(client)
    }

    private fun requireApi(): ApiClient =
        api ?: error("Not authenticated with a Jellyfin server")
}
