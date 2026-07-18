package pe.net.libre.mixtapehaven.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.data.download.VideoDownloadQuality
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.components.BackTopBar
import pe.net.libre.mixtapehaven.ui.components.SectionLabel
import pe.net.libre.mixtapehaven.ui.components.SettingToggleRow
import pe.net.libre.mixtapehaven.ui.components.StatusPill
import pe.net.libre.mixtapehaven.ui.components.SurfaceCard
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Coral
import pe.net.libre.mixtapehaven.ui.theme.Stroke
import pe.net.libre.mixtapehaven.ui.theme.Surface
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel {
        SettingsViewModel(
            it.sessionStore,
            it.downloadSettingsStore,
            it.downloadManager,
            it.videoDownloadManager,
            it.diagnosticsLog,
        )
    }
    val automaticDownloads by viewModel.autoDownloadEnabled.collectAsState()
    val storageUsed by viewModel.storageUsedLabel.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val serverHost by viewModel.serverHost.collectAsState()
    var wifiOnly by remember { mutableStateOf(true) }
    var showQualityPicker by remember { mutableStateOf(false) }

    if (showQualityPicker) {
        VideoQualityDialog(
            selected = videoQuality,
            onSelect = {
                viewModel.setVideoQuality(it)
                showQualityPicker = false
            },
            onDismiss = { showQualityPicker = false },
        )
    }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackTopBar(title = "Settings", onBack = onBack)

        // Account card
        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(1).uppercase().ifEmpty { "?" },
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentInk,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        serverHost,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                StatusPill("Online", Color(0xFF7BB661))
            }
        }

        // Downloads section
        SectionLabel("DOWNLOADS")
        SurfaceCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SettingToggleRow(
                    title = "Automatic downloads",
                    subtitle = "Keep songs for offline as you play them",
                    checked = automaticDownloads,
                    onCheckedChange = viewModel::setAutoDownloadEnabled,
                )
                HorizontalDivider(color = Stroke)
                SettingToggleRow(
                    title = "Download over Wi-Fi only",
                    subtitle = "Avoid using mobile data",
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it },
                )
                HorizontalDivider(color = Stroke)
                SettingLinkRow(title = "Audio quality", value = "Lossless", onClick = {})
                HorizontalDivider(color = Stroke)
                SettingLinkRow(
                    title = "Video download quality",
                    value = videoQuality.label,
                    onClick = { showQualityPicker = true },
                )
                HorizontalDivider(color = Stroke)
                SettingLinkRow(title = "Storage used", value = storageUsed, onClick = onOpenDownloads)
            }
        }

        // Account section
        SectionLabel("ACCOUNT")
        SurfaceCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SettingLinkRow(
                    title = "Jellyfin server",
                    value = serverHost,
                    onClick = {},
                )
                HorizontalDivider(color = Stroke)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOut)
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Sign out", style = MaterialTheme.typography.titleMedium, color = Coral)
                }
            }
        }

        // Diagnostics section
        SectionLabel("DIAGNOSTICS")
        SurfaceCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SettingLinkRow(
                    title = "Share diagnostics",
                    value = "Export",
                    onClick = { shareDiagnostics(context, viewModel.diagnosticsSnapshot()) },
                )
            }
        }

        Text(
            "Mixtape 1.0",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Fire a share sheet with the recent diagnostic [log] as plain text, for bug reports. */
private fun shareDiagnostics(context: Context, log: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Mixtape Haven diagnostics")
        putExtra(Intent.EXTRA_TEXT, log)
    }
    // No share target exists on some devices/restricted profiles; fall back to a toast, don't crash.
    try {
        context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share diagnostics", Toast.LENGTH_SHORT).show()
    }
}

/** Picker for the transcode cap applied to video downloads (1080p / 720p / 480p). */
@Composable
private fun VideoQualityDialog(
    selected: VideoDownloadQuality,
    onSelect: (VideoDownloadQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Video download quality", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
        text = {
            Column {
                VideoDownloadQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RadioButton(
                            selected = quality == selected,
                            onClick = { onSelect(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = TextMuted),
                        )
                        Text(quality.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "Done",
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
                modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
            )
        },
    )
}

@Composable
private fun SettingLinkRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
