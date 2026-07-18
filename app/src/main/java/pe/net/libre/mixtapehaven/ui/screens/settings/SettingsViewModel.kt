package pe.net.libre.mixtapehaven.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.URI
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.diagnostics.DiagnosticsLog
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadSettingsStore
import pe.net.libre.mixtapehaven.data.download.VideoDownloadManager
import pe.net.libre.mixtapehaven.data.download.VideoDownloadQuality
import pe.net.libre.mixtapehaven.data.download.formatBytes
import pe.net.libre.mixtapehaven.data.session.SessionStore

/** Backs the Settings screen: persists download preferences and surfaces real storage usage. */
class SettingsViewModel(
    sessionStore: SessionStore,
    private val settingsStore: DownloadSettingsStore,
    downloadManager: DownloadManager,
    videoDownloadManager: VideoDownloadManager,
    private val diagnostics: DiagnosticsLog,
) : ViewModel() {

    val userName: StateFlow<String> =
        sessionStore.session
            .map { it?.userName.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val serverHost: StateFlow<String> =
        sessionStore.session
            .map { it?.serverUrl?.toHostLabel().orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val autoDownloadEnabled: StateFlow<Boolean> =
        settingsStore.autoDownloadEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val storageUsedLabel: StateFlow<String> =
        combine(downloadManager.totalSizeBytes, videoDownloadManager.totalSizeBytes) { audio, video ->
            formatBytes(audio + video)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), formatBytes(0))

    val videoQuality: StateFlow<VideoDownloadQuality> =
        settingsStore.videoQuality
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), VideoDownloadQuality.DEFAULT)

    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoDownloadEnabled(enabled) }
    }

    fun setVideoQuality(quality: VideoDownloadQuality) {
        viewModelScope.launch { settingsStore.setVideoQuality(quality) }
    }

    /** A plain-text snapshot of the recent diagnostic events, for the Share diagnostics export. */
    fun diagnosticsSnapshot(): String = diagnostics.snapshot()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** The bare hostname for display (e.g. "jellyfin.home.lan"), falling back to the raw URL. */
private fun String.toHostLabel(): String =
    runCatching { URI(this).host }.getOrNull() ?: this
