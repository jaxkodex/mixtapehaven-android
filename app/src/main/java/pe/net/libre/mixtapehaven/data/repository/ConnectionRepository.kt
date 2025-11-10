package pe.net.libre.mixtapehaven.data.repository

import kotlinx.coroutines.delay
import pe.net.libre.mixtapehaven.data.model.ServerConnection
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager

class ConnectionRepository(private val dataStoreManager: DataStoreManager) {

    /**
     * Mock authentication function - validates input and simulates network delay
     * Returns true for successful connection, throws exception for errors
     */
    suspend fun authenticateConnection(connection: ServerConnection): Result<Boolean> {
        return try {
            // Validate server URL format
            if (!isValidUrl(connection.serverUrl)) {
                throw IllegalArgumentException("Invalid server URL format")
            }

            // Validate non-empty credentials
            if (connection.username.isBlank()) {
                throw IllegalArgumentException("Username cannot be empty")
            }

            if (connection.password.isBlank()) {
                throw IllegalArgumentException("Password cannot be empty")
            }

            // Simulate network delay
            delay(2000)

            // Mock authentication - always succeeds if validation passes
            dataStoreManager.saveConnection(
                connection.serverUrl,
                connection.username,
                connection.password
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
