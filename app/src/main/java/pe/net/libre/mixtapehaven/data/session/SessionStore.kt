package pe.net.libre.mixtapehaven.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A persisted, authenticated Jellyfin session. */
data class Session(
    val serverUrl: String,
    val accessToken: String,
    val userId: String,
    val userName: String,
)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Persists the current [Session] (server, token, user) across app launches via DataStore. */
class SessionStore(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val url = prefs[Keys.SERVER_URL]
        val token = prefs[Keys.ACCESS_TOKEN]
        val userId = prefs[Keys.USER_ID]
        if (url != null && token != null && userId != null) {
            Session(url, token, userId, prefs[Keys.USER_NAME].orEmpty())
        } else {
            null
        }
    }

    suspend fun save(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = session.serverUrl
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USER_NAME] = session.userName
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
