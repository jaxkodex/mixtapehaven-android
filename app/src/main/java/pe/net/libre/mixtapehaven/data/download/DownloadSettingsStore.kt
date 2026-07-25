package pe.net.libre.mixtapehaven.data.download

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadSettingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "download_settings")

/** Persists download preferences (auto-download, video quality) across app launches via DataStore. */
class DownloadSettingsStore(private val context: Context) {

    private object Keys {
        val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download_enabled")
        val VIDEO_QUALITY = stringPreferencesKey("video_download_quality")
        val WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
    }

    /** Whether tracks are saved for offline automatically as they play. Defaults to enabled. */
    val autoDownloadEnabled: Flow<Boolean> = context.downloadSettingsDataStore.data.map { prefs ->
        prefs[Keys.AUTO_DOWNLOAD] ?: true
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) {
        context.downloadSettingsDataStore.edit { it[Keys.AUTO_DOWNLOAD] = enabled }
    }

    /**
     * Whether downloads are restricted to unmetered (Wi-Fi) networks. Defaults to enabled:
     * video downloads are GB-sized transcodes. Gates video via a WorkManager constraint and
     * audio auto-download via a metered-network check at play time.
     */
    val wifiOnly: Flow<Boolean> = context.downloadSettingsDataStore.data.map { prefs ->
        prefs[Keys.WIFI_ONLY] ?: true
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.downloadSettingsDataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    /** The quality cap applied to video downloads. Defaults to [VideoDownloadQuality.DEFAULT]. */
    val videoQuality: Flow<VideoDownloadQuality> = context.downloadSettingsDataStore.data.map { prefs ->
        VideoDownloadQuality.fromName(prefs[Keys.VIDEO_QUALITY])
    }

    suspend fun setVideoQuality(quality: VideoDownloadQuality) {
        context.downloadSettingsDataStore.edit { it[Keys.VIDEO_QUALITY] = quality.name }
    }
}
