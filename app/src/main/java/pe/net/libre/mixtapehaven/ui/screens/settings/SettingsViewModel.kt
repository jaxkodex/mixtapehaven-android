package pe.net.libre.mixtapehaven.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadSettingsStore
import pe.net.libre.mixtapehaven.data.download.formatBytes

/** Backs the Settings screen: persists the auto-download toggle and surfaces real storage usage. */
class SettingsViewModel(
    private val settingsStore: DownloadSettingsStore,
    downloadManager: DownloadManager,
) : ViewModel() {

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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
