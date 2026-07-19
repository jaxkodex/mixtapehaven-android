package pe.net.libre.mixtapehaven.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.components.PosterCard
import pe.net.libre.mixtapehaven.ui.components.SearchField
import pe.net.libre.mixtapehaven.ui.components.SectionHeader
import pe.net.libre.mixtapehaven.ui.components.TrackRow
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel { SearchViewModel(it.repository, it.playerController) }
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf(TextFieldValue("")) }

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
                placeholder = "Search songs, movies, shows",
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it.text)
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                "Cancel",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        val countLabel = when {
            state.loading -> "Searching…"
            query.text.isBlank() -> "Type to search your library"
            else -> "${state.results.size + state.videos.size} results"
        }
        Text(countLabel, style = MaterialTheme.typography.labelMedium, color = TextMuted)

        // Video leads: a title match is a stronger intent signal than the song matches that
        // usually accompany it (soundtracks share the film's name).
        if (state.videos.isNotEmpty()) {
            SectionHeader("Movies & shows")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.videos, key = { it.id }) { video ->
                    PosterCard(
                        video = video,
                        onClick = { onOpenVideo(video.id) },
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

        if (state.results.isNotEmpty()) {
            SectionHeader(
                "Songs",
                actionLabel = "Play all",
                onAction = {
                    viewModel.playAll()
                    onOpenNowPlaying()
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.results.forEachIndexed { index, track ->
                    TrackRow(
                        track,
                        onClick = {
                            viewModel.playFrom(index)
                            onOpenNowPlaying()
                        },
                    )
                }
            }
        }
    }
}
