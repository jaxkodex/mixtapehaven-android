package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadSettingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "download_settings")

/** Persists download preferences (auto-download enabled) across app launches via DataStore. */
class DownloadSettingsStore(private val context: Context) {

    private object Keys {
        val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download_enabled")
    }

    /** Whether tracks are saved for offline automatically as they play. Defaults to enabled. */
    val autoDownloadEnabled: Flow<Boolean> = context.downloadSettingsDataStore.data.map { prefs ->
        prefs[Keys.AUTO_DOWNLOAD] ?: true
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) {
        context.downloadSettingsDataStore.edit { it[Keys.AUTO_DOWNLOAD] = enabled }
    }
}
