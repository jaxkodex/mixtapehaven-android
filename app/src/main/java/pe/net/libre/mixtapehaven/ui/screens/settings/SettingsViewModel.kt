package pe.net.libre.mixtapehaven.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.URI
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.diagnostics.DiagnosticsLog
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadSettingsStore
import pe.net.libre.mixtapehaven.data.download.formatBytes
import pe.net.libre.mixtapehaven.data.session.SessionStore

/** Backs the Settings screen: persists the auto-download toggle and surfaces real storage usage. */
class SettingsViewModel(
    sessionStore: SessionStore,
    private val settingsStore: DownloadSettingsStore,
    downloadManager: DownloadManager,
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
        downloadManager.totalSizeBytes
            .map { formatBytes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), formatBytes(0))

    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoDownloadEnabled(enabled) }
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
