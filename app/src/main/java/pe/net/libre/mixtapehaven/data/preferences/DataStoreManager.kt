package pe.net.libre.mixtapehaven.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mixtape_haven_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val PASSWORD_KEY = stringPreferencesKey("password")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val SERVER_ID_KEY = stringPreferencesKey("server_id")
        private val IS_CONNECTED_KEY = stringPreferencesKey("is_connected")

        // Download preferences
        private val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
        private val MAX_CACHE_SIZE_KEY = longPreferencesKey("max_cache_size")
        private val WIFI_ONLY_DOWNLOAD_KEY = booleanPreferencesKey("wifi_only_download")
    }

    val serverUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_URL_KEY]
        }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USERNAME_KEY]
        }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY]
        }

    val serverId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_ID_KEY]
        }

    val isConnected: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_CONNECTED_KEY]?.toBoolean() ?: false
        }

    suspend fun saveConnection(
        serverUrl: String,
        username: String,
        password: String,
        accessToken: String,
        userId: String,
        serverId: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = serverUrl
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[USER_ID_KEY] = userId
            preferences[SERVER_ID_KEY] = serverId
            preferences[IS_CONNECTED_KEY] = "true"
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(PASSWORD_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(SERVER_ID_KEY)
            preferences[IS_CONNECTED_KEY] = "false"
        }
    }

    // Download preferences
    val downloadQuality: Flow<StreamingQuality> = context.dataStore.data
        .map { preferences ->
            val qualityName = preferences[DOWNLOAD_QUALITY_KEY] ?: "HIGH"
            try {
                StreamingQuality.valueOf(qualityName)
            } catch (e: IllegalArgumentException) {
                StreamingQuality.HIGH
            }
        }

    val maxCacheSize: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[MAX_CACHE_SIZE_KEY] ?: 2_000_000_000L // 2GB default
        }

    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[WIFI_ONLY_DOWNLOAD_KEY] ?: true // Default: WiFi only
        }

    suspend fun saveDownloadQuality(quality: StreamingQuality) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY_KEY] = quality.name
        }
    }

    suspend fun saveMaxCacheSize(size: Long) {
        context.dataStore.edit { preferences ->
            preferences[MAX_CACHE_SIZE_KEY] = size
        }
    }

    suspend fun saveWifiOnlyDownload(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_DOWNLOAD_KEY] = wifiOnly
        }
    }
}
