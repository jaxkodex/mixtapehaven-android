package pe.net.libre.mixtapehaven.ui.screens.settings

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.SampleData
import pe.net.libre.mixtapehaven.ui.components.BackTopBar
import pe.net.libre.mixtapehaven.ui.components.SectionLabel
import pe.net.libre.mixtapehaven.ui.components.SettingToggleRow
import pe.net.libre.mixtapehaven.ui.components.StatusPill
import pe.net.libre.mixtapehaven.ui.components.SurfaceCard
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Coral
import pe.net.libre.mixtapehaven.ui.theme.Stroke
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
    var automaticDownloads by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }

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
                    Text("A", style = MaterialTheme.typography.titleLarge, color = AccentInk)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        SampleData.USER_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        SampleData.SERVER_HOST,
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
                    onCheckedChange = { automaticDownloads = it },
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
                SettingLinkRow(title = "Storage used", value = "1.2 GB", onClick = onOpenDownloads)
            }
        }

        // Account section
        SectionLabel("ACCOUNT")
        SurfaceCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SettingLinkRow(
                    title = "Jellyfin server",
                    value = SampleData.SERVER_HOST,
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

        Text(
            "Mixtape 1.0",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
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
