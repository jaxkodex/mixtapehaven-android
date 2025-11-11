package pe.net.libre.mixtapehaven.data.repository

import android.content.Context
import android.provider.Settings
import pe.net.libre.mixtapehaven.data.api.AuthenticationRequest
import pe.net.libre.mixtapehaven.data.api.JellyfinApiClient
import pe.net.libre.mixtapehaven.data.model.ServerConnection
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ConnectionRepository(
    private val dataStoreManager: DataStoreManager,
    private val context: Context
) {

    /**
     * Authenticate with Jellyfin server
     * Returns true for successful connection, throws exception for errors
     */
    suspend fun authenticateConnection(connection: ServerConnection): Result<Boolean> {
        return try {
            // Validate server URL format
            if (!isValidUrl(connection.serverUrl)) {
                throw IllegalArgumentException("Invalid server URL format. Must start with http:// or https://")
            }

            // Validate non-empty credentials
            if (connection.username.isBlank()) {
                throw IllegalArgumentException("Username cannot be empty")
            }

            if (connection.password.isBlank()) {
                throw IllegalArgumentException("Password cannot be empty")
            }

            // Create API service for the provided server URL
            val apiService = JellyfinApiClient.createService(connection.serverUrl)

            // First, verify server connectivity by fetching public system info
            try {
                val systemInfo = apiService.getPublicSystemInfo()
                // Successfully connected to server
            } catch (e: Exception) {
                val errorDetails = buildString {
                    append("\n\nTechnical Details:\n")
                    append("Error Type: ${e.javaClass.simpleName}\n")
                    append("Message: ${e.message ?: "No message available"}\n")
                    e.cause?.let { cause ->
                        append("Cause: ${cause.javaClass.simpleName}: ${cause.message}\n")
                    }
                    append("Server URL: ${connection.serverUrl}")
                }

                throw when (e) {
                    is UnknownHostException -> IllegalArgumentException(
                        "Cannot reach server. Check the URL.$errorDetails"
                    )
                    is ConnectException -> IllegalArgumentException(
                        "Cannot connect to server. Check if it's running.$errorDetails"
                    )
                    is SocketTimeoutException -> IllegalArgumentException(
                        "Connection timeout. Server took too long to respond.$errorDetails"
                    )
                    else -> IllegalArgumentException(
                        "Failed to connect to server.$errorDetails"
                    )
                }
            }

            // Generate device ID from Android ID
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            // Create authorization header
            val authHeader = JellyfinApiClient.createAuthorizationHeader(
                deviceId = deviceId
            )

            // Authenticate with credentials
            val authRequest = AuthenticationRequest(
                username = connection.username,
                password = connection.password
            )

            val authResponse = try {
                apiService.authenticateByName(authRequest, authHeader)
            } catch (e: Exception) {
                val errorDetails = buildString {
                    append("\n\nTechnical Details:\n")
                    append("Error Type: ${e.javaClass.simpleName}\n")
                    append("Message: ${e.message ?: "No message available"}\n")
                    e.cause?.let { cause ->
                        append("Cause: ${cause.javaClass.simpleName}: ${cause.message}\n")
                    }
                    append("Server URL: ${connection.serverUrl}\n")
                    append("Username: ${connection.username}")
                }

                throw IllegalArgumentException(
                    "Authentication failed: Invalid username or password.$errorDetails"
                )
            }

            // Save connection details including access token
            dataStoreManager.saveConnection(
                serverUrl = connection.serverUrl,
                username = connection.username,
                password = connection.password,
                accessToken = authResponse.accessToken,
                userId = authResponse.user.id,
                serverId = authResponse.serverId
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearConnection() {
        dataStoreManager.clearConnection()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() &&
               (url.startsWith("http://") || url.startsWith("https://"))
    }
}
