package pe.net.libre.mixtapehaven.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JellyfinApiService {

    /**
     * Get system information to verify server connectivity
     */
    @GET("System/Info/Public")
    suspend fun getPublicSystemInfo(): SystemInfo

    /**
     * Authenticate user with username and password
     */
    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body request: AuthenticationRequest,
        @Header("X-Emby-Authorization") authHeader: String
    ): AuthenticationResponse

    /**
     * Get items from the user's library
     */
    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean? = true,
        @Query("SortBy") sortBy: String? = null,
        @Query("SortOrder") sortOrder: String? = null,
        @Query("Limit") limit: Int? = null,
        @Query("StartIndex") startIndex: Int? = null,
        @Query("Fields") fields: String? = null,
        @Query("ParentId") parentId: String? = null,
        @Query("ImageTypeLimit") imageTypeLimit: Int? = 1,
        @Query("EnableImageTypes") enableImageTypes: String? = "Primary,Backdrop,Thumb",
        @Query("Ids") ids: String? = null,
        @Query("ArtistIds") artistIds: String? = null,
        @Query("SearchTerm") searchTerm: String? = null
    ): ItemsResponse

    /**
     * Get latest media items
     */
    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestMedia(
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Limit") limit: Int? = null,
        @Query("Fields") fields: String? = null,
        @Query("ImageTypeLimit") imageTypeLimit: Int? = 1,
        @Query("EnableImageTypes") enableImageTypes: String? = "Primary,Backdrop,Thumb"
    ): List<BaseItemDto>

    /**
     * Get search hints for autocomplete
     */
    @GET("Search/Hints")
    suspend fun searchHints(
        @Query("UserId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("Limit") limit: Int? = 10,
        @Query("IncludeItemTypes") includeItemTypes: String? = null
    ): SearchHintResult

    /**
     * Create a new playlist
     */
    @POST("Playlists")
    suspend fun createPlaylist(
        @Body request: CreatePlaylistRequest,
        @Header("X-Emby-Authorization") authHeader: String? = null
    ): PlaylistCreationResult

    /**
     * Add items to an existing playlist
     */
    @POST("Playlists/{playlistId}/Items")
    suspend fun addToPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("Ids") ids: String,
        @Query("UserId") userId: String
    )
}
