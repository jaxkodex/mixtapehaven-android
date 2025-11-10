package pe.net.libre.mixtapehaven.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mixtape_haven_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val PASSWORD_KEY = stringPreferencesKey("password")
        private val IS_CONNECTED_KEY = stringPreferencesKey("is_connected")
    }

    val serverUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_URL_KEY]
        }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USERNAME_KEY]
        }

    val isConnected: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_CONNECTED_KEY]?.toBoolean() ?: false
        }

    suspend fun saveConnection(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = serverUrl
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
            preferences[IS_CONNECTED_KEY] = "true"
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(PASSWORD_KEY)
            preferences[IS_CONNECTED_KEY] = "false"
        }
    }
}
