package pe.net.libre.mixtapehaven.data.jellyfin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.dynamicHlsApi
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetGenresRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import pe.net.libre.mixtapehaven.data.session.Session
import pe.net.libre.mixtapehaven.data.session.SessionStore
import pe.net.libre.mixtapehaven.model.Album
import pe.net.libre.mixtapehaven.model.Track
import pe.net.libre.mixtapehaven.model.VideoItem
import java.util.UUID

/** Lifecycle moment of video playback reported to the server via [JellyfinRepository.reportVideoPlayback]. */
enum class VideoPlaybackEvent { STARTED, PROGRESS, STOPPED }

/**
 * Image types requested for Continue watching / Next Up rows. BACKDROP and THUMB are the wide art
 * the 16:9 still wants; PRIMARY is the fallback (and is itself a 16:9 still for episodes).
 */
private val CONTINUE_IMAGE_TYPES = listOf(ImageType.BACKDROP, ImageType.THUMB, ImageType.PRIMARY)

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

    /** Full detail (overview + this user's resume position) for one movie/series/episode. */
    suspend fun videoItem(itemId: String): VideoItem? {
        val client = requireApi()
        val id = runCatching { UUID.fromString(itemId) }.getOrNull() ?: return null
        val item by client.userLibraryApi.getItem(itemId = id, userId = userId)
        return item.toVideoItem(client)
    }

    /**
     * All episodes of [seriesId] in season/episode order, with overviews and resume positions.
     *
     * The sort is applied here rather than trusting the endpoint's default: autoplay picks the
     * next episode positionally, so the ordering is load-bearing and should not be an implicit
     * assumption about server behaviour.
     */
    suspend fun seriesEpisodes(seriesId: String): List<VideoItem> {
        val client = requireApi()
        val id = runCatching { UUID.fromString(seriesId) }.getOrNull() ?: return emptyList()
        val result by client.tvShowsApi.getEpisodes(
            seriesId = id,
            userId = userId,
            fields = listOf(ItemFields.OVERVIEW),
            enableUserData = true,
        )
        return result.items.orEmpty()
            .map { it.toVideoItem(client) to it }
            .sortedWith(compareBy({ it.second.parentIndexNumber ?: 0 }, { it.second.indexNumber ?: 0 }))
            .map { it.first }
    }

    /**
     * The id of the media source the server picked for [itemId], via a PlaybackInfo negotiation.
     *
     * Transcode URLs need a real MediaSource id. For a plain single-file library item that happens
     * to equal the item id unhyphenated, but .strm-backed items have their own, and guessing sends
     * the server a source it does not know. Falls back to the guess when the call fails, so a
     * negotiation error degrades to the old behaviour rather than blocking playback.
     *
     * Takes the first source the server lists, which is *not* a considered choice between the
     * versions of a multi-version item (a 4K and a 1080p cut) — picking by requested quality would
     * mean matching against each source's streams. Note also that this is an extra POST on the
     * playback critical path, and that it opens a server-side play session.
     */
    suspend fun mediaSourceId(itemId: String): String {
        val fallback = itemId.replace("-", "")
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return fallback
        return runCatching {
            val info by client.mediaInfoApi.getPostedPlaybackInfo(
                itemId = id,
                data = PlaybackInfoDto(userId = userId, enableDirectPlay = true, enableTranscoding = true),
            )
            info.mediaSources.firstOrNull()?.id
        }.getOrNull() ?: fallback
    }

    /**
     * Ordered stream URL candidates for [itemId]: direct play of the original file first, then an
     * HLS transcode pinned to h264/aac for anything the device cannot play natively. The player
     * tries them in order and falls through on error.
     */
    suspend fun videoStreamCandidates(itemId: String): List<String> {
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return emptyList()
        val direct = client.videosApi
            .getVideoStreamUrl(itemId = id, static = true)
            .withApiKey(client)
        val hls = client.dynamicHlsApi
            .getMasterHlsVideoPlaylistUrl(
                itemId = id,
                mediaSourceId = mediaSourceId(itemId),
                videoCodec = "h264",
                audioCodec = "aac",
            )
            .withApiKey(client)
        return listOf(direct, hls)
    }

    /**
     * The next episode to watch in [seriesId] per the server's Next Up logic, or null when the
     * series is finished or unwatched. Unlike scanning episodes for a resume position this is
     * correct after an episode is watched to completion (Jellyfin zeroes its position, so a local
     * scan would send the user back to the episode they just finished).
     */
    suspend fun nextUpEpisode(seriesId: String): VideoItem? {
        val client = requireApi()
        val id = runCatching { UUID.fromString(seriesId) }.getOrNull() ?: return null
        val result by client.tvShowsApi.getNextUp(
            GetNextUpRequest(
                userId = userId,
                seriesId = id,
                fields = listOf(ItemFields.OVERVIEW),
                limit = 1,
                // Include the partially-watched episode itself, not just the one after it.
                enableResumable = true,
                enableImages = true,
                enableImageTypes = CONTINUE_IMAGE_TYPES,
            ),
        )
        return result.items.orEmpty().firstOrNull()?.toVideoItem(client)
    }

    /** The server's Continue Watching list: partially-watched movies and episodes, most recent first. */
    suspend fun continueWatching(limit: Int = 12): List<VideoItem> {
        val client = requireApi()
        val result by client.itemsApi.getResumeItems(
            GetResumeItemsRequest(
                userId = userId,
                limit = limit,
                mediaTypes = listOf(MediaType.VIDEO),
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
                fields = listOf(ItemFields.OVERVIEW),
                // Resume items come back without image tags unless asked for, which would leave
                // every Continue watching still on its flat colour fallback.
                enableImages = true,
                enableImageTypes = CONTINUE_IMAGE_TYPES,
            ),
        )
        return result.items.orEmpty().map { it.toVideoItem(client) }
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
    suspend fun videoDownloadUrl(itemId: String, maxHeight: Int, videoBitRate: Int, audioBitRate: Int): String? {
        val client = api
        val id = runCatching { UUID.fromString(itemId) }.getOrNull()
        if (client == null || id == null) return null
        return client.videosApi
            .getVideoStreamUrl(
                itemId = id,
                container = "ts",
                static = false,
                mediaSourceId = mediaSourceId(itemId),
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

    /** The authenticated client and current user, for query classes built on this session. */
    internal fun authedClient(): Pair<ApiClient, UUID?> = requireApi() to userId
}
