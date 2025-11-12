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
        @Query("EnableImageTypes") enableImageTypes: String? = "Primary,Backdrop,Thumb"
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
}
