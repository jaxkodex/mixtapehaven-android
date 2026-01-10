package pe.net.libre.mixtapehaven.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.cache.CacheStatistics
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val offlineRepository: OfflineRepository
) : ViewModel() {

    private val _cacheStatistics = MutableStateFlow<CacheStatistics?>(null)
    val cacheStatistics: StateFlow<CacheStatistics?> = _cacheStatistics.asStateFlow()

    private val _downloadQuality = MutableStateFlow(StreamingQuality.HIGH)
    val downloadQuality: StateFlow<StreamingQuality> = _downloadQuality.asStateFlow()

    private val _maxCacheSize = MutableStateFlow(2_000_000_000L) // 2GB default
    val maxCacheSize: StateFlow<Long> = _maxCacheSize.asStateFlow()

    private val _wifiOnlyDownload = MutableStateFlow(true)
    val wifiOnlyDownload: StateFlow<Boolean> = _wifiOnlyDownload.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    init {
        loadSettings()
        loadCacheStatistics()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            dataStoreManager.downloadQuality.collect { quality ->
                _downloadQuality.value = quality
            }
        }

        viewModelScope.launch {
            dataStoreManager.maxCacheSize.collect { size ->
                _maxCacheSize.value = size
            }
        }

        viewModelScope.launch {
            dataStoreManager.wifiOnlyDownload.collect { wifiOnly ->
                _wifiOnlyDownload.value = wifiOnly
            }
        }
    }

    private fun loadCacheStatistics() {
        viewModelScope.launch {
            try {
                val stats = offlineRepository.getCacheStatistics()
                _cacheStatistics.value = stats
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setDownloadQuality(quality: StreamingQuality) {
        viewModelScope.launch {
            dataStoreManager.saveDownloadQuality(quality)
            _downloadQuality.value = quality
        }
    }

    fun setMaxCacheSize(size: Long) {
        viewModelScope.launch {
            dataStoreManager.saveMaxCacheSize(size)
            _maxCacheSize.value = size
            loadCacheStatistics() // Refresh statistics
        }
    }

    fun setWifiOnlyDownload(wifiOnly: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveWifiOnlyDownload(wifiOnly)
            _wifiOnlyDownload.value = wifiOnly
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearing.value = true
            try {
                offlineRepository.clearCache()
                loadCacheStatistics() // Refresh statistics
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isClearing.value = false
            }
        }
    }

    fun refreshCacheStatistics() {
        loadCacheStatistics()
    }
}
