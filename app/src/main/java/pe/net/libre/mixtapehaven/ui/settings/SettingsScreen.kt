package pe.net.libre.mixtapehaven.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.ui.home.StreamingQuality
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val cacheStatistics by viewModel.cacheStatistics.collectAsState()
    val downloadQuality by viewModel.downloadQuality.collectAsState()
    val maxCacheSize by viewModel.maxCacheSize.collectAsState()
    val wifiOnlyDownload by viewModel.wifiOnlyDownload.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack,
                    titleContentColor = LunarWhite,
                    navigationIconContentColor = LunarWhite
                )
            )
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Download Quality Section
            SettingsSectionTitle("Download Quality")
            QualityDropdown(
                selectedQuality = downloadQuality,
                onQualitySelected = { viewModel.setDownloadQuality(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cache Size Section
            SettingsSectionTitle("Max Cache Size")
            CacheSizeSlider(
                currentSize = maxCacheSize,
                onSizeChanged = { viewModel.setMaxCacheSize(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // WiFi Only Section
            SettingsSectionTitle("Download Preferences")
            WifiOnlySetting(
                enabled = wifiOnlyDownload,
                onToggle = { viewModel.setWifiOnlyDownload(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cache Statistics Section
            SettingsSectionTitle("Storage Usage")
            cacheStatistics?.let { stats ->
                CacheStatisticsCard(stats)
            }

            // Clear Cache Button
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearing && (cacheStatistics?.songCount ?: 0) > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(if (isClearing) "Clearing..." else "Clear Cache")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out link
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign Out",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Cache?") },
            text = {
                Text(
                    "This will delete all downloaded songs (${cacheStatistics?.songCount ?: 0} songs). " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = CyberNeonBlue
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityDropdown(
    selectedQuality: StreamingQuality,
    onQualitySelected: (StreamingQuality) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = getQualityDisplayName(selectedQuality),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            StreamingQuality.entries.forEach { quality ->
                DropdownMenuItem(
                    text = { Text(getQualityDisplayName(quality)) },
                    onClick = {
                        onQualitySelected(quality)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CacheSizeSlider(
    currentSize: Long,
    onSizeChanged: (Long) -> Unit
) {
    val sizeInGB = currentSize / (1024f * 1024f * 1024f)

    Column {
        Text(
            text = "%.1f GB".format(sizeInGB),
            style = MaterialTheme.typography.bodyLarge,
            color = LunarWhite,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = sizeInGB,
            onValueChange = {
                val bytes = (it * 1024 * 1024 * 1024).toLong()
                onSizeChanged(bytes)
            },
            valueRange = 0.5f..10f,
            steps = 18, // 0.5GB increments
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.5 GB", style = MaterialTheme.typography.bodySmall, color = GunmetalGray)
            Text("10 GB", style = MaterialTheme.typography.bodySmall, color = GunmetalGray)
        }
    }
}

@Composable
private fun WifiOnlySetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "WiFi Only Downloads",
                style = MaterialTheme.typography.bodyLarge,
                color = LunarWhite
            )
            Text(
                text = "Only download when connected to WiFi",
                style = MaterialTheme.typography.bodySmall,
                color = GunmetalGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun CacheStatisticsCard(stats: pe.net.libre.mixtapehaven.data.cache.CacheStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GunmetalGray.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Usage bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stats.totalSizeFormatted} / ${stats.maxSizeFormatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunarWhite
                    )
                    Text(
                        text = "%.1f%%".format(stats.usagePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberNeonBlue
                    )
                }

                LinearProgressIndicator(
                    progress = { stats.usagePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(8.dp)
                        .background(GunmetalGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    color = CyberNeonBlue,
                )
            }

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem("Songs", stats.songCount.toString())
                StatisticItem("Available", stats.availableSizeFormatted)
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GunmetalGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = LunarWhite
        )
    }
}

private fun getQualityDisplayName(quality: StreamingQuality): String {
    return when (quality) {
        StreamingQuality.ORIGINAL -> "Original (Lossless)"
        StreamingQuality.HIGH -> "High (320 kbps)"
        StreamingQuality.MEDIUM -> "Medium (192 kbps)"
        StreamingQuality.LOW -> "Low (128 kbps)"
    }
}
