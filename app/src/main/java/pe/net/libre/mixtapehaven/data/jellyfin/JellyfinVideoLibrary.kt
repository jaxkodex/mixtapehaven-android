package pe.net.libre.mixtapehaven.data.jellyfin

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetGenresRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import pe.net.libre.mixtapehaven.model.VideoItem

/** Items per page in the video library grid. */
const val VIDEO_PAGE_SIZE = 36

/** Which kinds of video the library screen is showing. */
enum class VideoFilter { ALL, MOVIES, SERIES }

/** Ordering offered by the library screen, mapped to Jellyfin sort keys in [VideoSort.sortBy]. */
enum class VideoSort { RECENTLY_ADDED, TITLE, YEAR, RATING }

/**
 * One page of library results. [totalCount] is the size of the whole filtered set, so the caller
 * knows whether another page exists without having to fetch an empty one.
 */
data class VideoPage(
    val items: List<VideoItem>,
    val totalCount: Int,
)

/**
 * Browse and search over the video libraries.
 *
 * An interface so the paging/facet state machine in the library ViewModel can be driven by a fake:
 * the real implementation needs a live [ApiClient] and cannot be constructed off-device.
 */
interface VideoLibrarySource {
    suspend fun videoLibrary(
        filter: VideoFilter = VideoFilter.ALL,
        genre: String? = null,
        sort: VideoSort = VideoSort.RECENTLY_ADDED,
        startIndex: Int = 0,
        limit: Int = VIDEO_PAGE_SIZE,
    ): VideoPage

    suspend fun videoGenres(limit: Int = 60): List<String>

    suspend fun searchVideos(query: String, limit: Int = 24): List<VideoItem>
}

/** Item types requested per [VideoFilter]. */
private fun VideoFilter.itemTypes(): List<BaseItemKind> = when (this) {
    VideoFilter.ALL -> listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
    VideoFilter.MOVIES -> listOf(BaseItemKind.MOVIE)
    VideoFilter.SERIES -> listOf(BaseItemKind.SERIES)
}

/**
 * Sort keys per [VideoSort]. Each falls back to SORT_NAME so items the primary key cannot separate
 * (no year, no rating) come back alphabetically instead of in server order, which is arbitrary and
 * changes between pages — that would make paging duplicate and drop items.
 */
private fun VideoSort.sortBy(): List<ItemSortBy> = when (this) {
    VideoSort.RECENTLY_ADDED -> listOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME)
    VideoSort.TITLE -> listOf(ItemSortBy.SORT_NAME)
    VideoSort.YEAR -> listOf(ItemSortBy.PRODUCTION_YEAR, ItemSortBy.SORT_NAME)
    VideoSort.RATING -> listOf(ItemSortBy.COMMUNITY_RATING, ItemSortBy.SORT_NAME)
}

/**
 * Sort directions, paired positionally with [sortBy]. The primary key descends (newest, highest,
 * most recent first) except for TITLE; the SORT_NAME tiebreak always ascends.
 *
 * Both entries are spelled out rather than relying on Jellyfin defaulting the unmatched key to
 * ascending: paging stability rests on that tiebreak, so it should not depend on server behaviour
 * this code never states.
 */
private fun VideoSort.sortOrder(): List<SortOrder> = when (this) {
    VideoSort.TITLE -> listOf(SortOrder.ASCENDING)
    else -> listOf(SortOrder.DESCENDING, SortOrder.ASCENDING)
}

/** Fields the video grid and detail header need beyond the defaults. */
private val VIDEO_FIELDS = listOf(ItemFields.OVERVIEW, ItemFields.GENRES)

/**
 * The video browse/search queries, split out of [JellyfinRepository] so that class stays a facade
 * over auth and playback rather than growing a third responsibility.
 *
 * Takes the authenticated client from [repository] per call instead of holding one: the client is
 * replaced on sign-in and cleared on sign-out, so a captured reference would go stale.
 */
class JellyfinVideoLibrary(
    private val repository: JellyfinRepository,
) : VideoLibrarySource {

    /**
     * One page of the video library, filtered by [filter] and optionally by [genre], ordered by
     * [sort].
     *
     * Paging is offset-based ([startIndex]), which is only stable because every [VideoSort] pins a
     * total order — see [VideoSort.sortBy].
     */
    override suspend fun videoLibrary(
        filter: VideoFilter,
        genre: String?,
        sort: VideoSort,
        startIndex: Int,
        limit: Int,
    ): VideoPage {
        val (client, userId) = repository.authedClient()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = filter.itemTypes(),
                recursive = true,
                sortBy = sort.sortBy(),
                sortOrder = sort.sortOrder(),
                genres = genre?.let { listOf(it) },
                fields = VIDEO_FIELDS,
                enableUserData = true,
                startIndex = startIndex,
                limit = limit,
                enableTotalRecordCount = true,
            ),
        )
        return VideoPage(
            items = result.items.map { it.toVideoItem(client) },
            totalCount = result.totalRecordCount,
        )
    }

    /**
     * Genre names present in the user's movie and series libraries, for the library filter chips.
     * Returns empty rather than throwing when the server has no video libraries at all.
     */
    override suspend fun videoGenres(limit: Int): List<String> = runCatching {
        val (client, userId) = repository.authedClient()
        val result by client.genresApi.getGenres(
            GetGenresRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                sortBy = listOf(ItemSortBy.SORT_NAME),
                sortOrder = listOf(SortOrder.ASCENDING),
                limit = limit,
                enableImages = false,
            ),
        )
        result.items.mapNotNull { it.name }
    }.getOrDefault(emptyList())

    /**
     * Movies, series, and episodes matching [query], for the video section of search.
     *
     * Episodes are included because users search for an episode title as readily as a series one;
     * the caller routes an episode hit to its own detail entry.
     */
    override suspend fun searchVideos(query: String, limit: Int): List<VideoItem> {
        val (client, userId) = repository.authedClient()
        val result by client.itemsApi.getItems(
            GetItemsRequest(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE),
                recursive = true,
                searchTerm = query,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                fields = VIDEO_FIELDS,
                enableUserData = true,
                limit = limit,
            ),
        )
        return result.items.map { it.toVideoItem(client) }
    }
}
