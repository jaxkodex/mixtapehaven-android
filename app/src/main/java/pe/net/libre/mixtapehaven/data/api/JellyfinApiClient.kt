package pe.net.libre.mixtapehaven.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object JellyfinApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createService(baseUrl: String): JellyfinApiService {
        // Ensure base URL ends with a slash
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(JellyfinApiService::class.java)
    }

    /**
     * Generate the X-Emby-Authorization header required by Jellyfin
     * Format: MediaBrowser Client="client_name", Device="device_name",
     *         DeviceId="device_id", Version="version"
     */
    fun createAuthorizationHeader(
        clientName: String = "Mixtape Haven",
        deviceName: String = "Android",
        deviceId: String,
        version: String = "1.0.0"
    ): String {
        return "MediaBrowser Client=\"$clientName\", Device=\"$deviceName\", " +
               "DeviceId=\"$deviceId\", Version=\"$version\""
    }
}
