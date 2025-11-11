package pe.net.libre.mixtapehaven.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
}
