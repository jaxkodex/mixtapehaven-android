package pe.net.libre.mixtapehaven.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.SampleData
import pe.net.libre.mixtapehaven.ui.components.CircularPlayButton
import pe.net.libre.mixtapehaven.ui.components.FilterChipRow
import pe.net.libre.mixtapehaven.ui.components.SearchField
import pe.net.libre.mixtapehaven.ui.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.components.SurfaceCard
import pe.net.libre.mixtapehaven.ui.components.TrackRow
import pe.net.libre.mixtapehaven.ui.components.VinylArt
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun SearchScreen(onBack: () -> Unit, onOpenNowPlaying: () -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf(TextFieldValue("moon")) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(
                placeholder = "Search",
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
            )
            Text(
                "Cancel",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        Text(
            "8 results · 4 in queue",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )

        FilterChipRow(
            options = listOf("Songs", "Albums", "Artists", "Playlists"),
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
        )

        Text("Top result", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        SurfaceCard {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VinylArt(SampleData.topResult.artColor, Modifier.size(64.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        SampleData.topResult.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        "ALBUM · THE REVERIE · 1983",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                CircularPlayButton(onClick = onOpenNowPlaying)
            }
        }

        SectionHeader("Songs", actionLabel = "Play all", onAction = onOpenNowPlaying)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SampleData.searchResults.forEach { track ->
                TrackRow(track, onClick = onOpenNowPlaying)
            }
        }
    }
}
