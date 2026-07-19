package pe.net.libre.mixtapehaven.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.net.libre.mixtapehaven.data.jellyfin.VideoFilter
import pe.net.libre.mixtapehaven.data.jellyfin.VideoSort
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.components.PosterCard
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Stroke
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

/** Poster columns in the library grid. */
private const val GRID_COLUMNS = 3

/** How close to the end of the grid triggers the next page fetch. */
private const val PREFETCH_DISTANCE = 6

/** Browsable, filterable, paged grid of every movie and series in the user's libraries. */
@Composable
fun VideoLibraryScreen(
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel { VideoLibraryViewModel(it.videoLibrary) }
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState()

    // Paging is driven off the last visible index rather than an onClick "Load more" so the grid
    // fills ahead of the user; distinctUntilChanged keeps every scroll pixel from re-asking.
    //
    // Only the index is de-duplicated, but the condition also depends on state.items.size — that
    // is read through the composable's State, so an appended page re-evaluates the comparison on
    // the next emission without needing to be part of the key.
    LaunchedEffect(gridState, viewModel) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= state.items.size - PREFETCH_DISTANCE) viewModel.loadMore()
            }
    }

    // A facet change replaces the list from index 0; without this the grid keeps its old offset
    // and lands the user mid-list in a set they never scrolled.
    LaunchedEffect(state.facetGeneration) {
        gridState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LibraryHeader(
            sort = state.sort,
            onBack = onBack,
            onSort = viewModel::setSort,
        )
        FilterChips(selected = state.filter, onSelect = viewModel::setFilter)
        if (state.genres.isNotEmpty()) {
            GenreChips(
                genres = state.genres,
                selected = state.genre,
                onSelect = viewModel::setGenre,
            )
        }

        when {
            state.loading && state.items.isEmpty() -> CenteredMessage("Loading your library…")
            state.error != null && state.items.isEmpty() -> CenteredMessage(
                state.error ?: "",
                actionLabel = "Retry",
                onAction = viewModel::retry,
            )
            state.items.isEmpty() -> CenteredMessage("Nothing matches these filters.")
            else -> PosterGrid(
                state = state,
                gridState = gridState,
                onOpenVideo = onOpenVideo,
            )
        }
    }
}

@Composable
private fun PosterGrid(
    state: VideoLibraryViewModel.UiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenVideo: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.items, key = { it.id }) { video ->
            PosterCard(video = video, onClick = { onOpenVideo(video.id) })
        }
        if (state.loadingMore) {
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Accent,
                        trackColor = Surface2,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    sort: VideoSort,
    onBack: () -> Unit,
    onSort: (VideoSort) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            "Movies & shows",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        SortMenu(sort = sort, onSort = onSort)
    }
}

/** Sort affordance: an icon that opens the ordering choices, with the active one accented. */
@Composable
private fun SortMenu(sort: VideoSort, onSort: (VideoSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.SwapVert,
                contentDescription = "Sort",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VideoSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label(),
                            color = if (option == sort) Accent else TextPrimary,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSort(option)
                    },
                )
            }
        }
    }
}

private fun VideoSort.label(): String = when (this) {
    VideoSort.RECENTLY_ADDED -> "Recently added"
    VideoSort.TITLE -> "Title"
    VideoSort.YEAR -> "Year"
    VideoSort.RATING -> "Rating"
}

private fun VideoFilter.label(): String = when (this) {
    VideoFilter.ALL -> "All"
    VideoFilter.MOVIES -> "Movies"
    VideoFilter.SERIES -> "Series"
}

@Composable
private fun FilterChips(selected: VideoFilter, onSelect: (VideoFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VideoFilter.entries.forEach { filter ->
            Chip(
                label = filter.label(),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun GenreChips(genres: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(genres, key = { it }) { genre ->
            Chip(
                label = genre,
                selected = genre == selected,
                onClick = { onSelect(genre) },
            )
        }
    }
}

/** Pill toggle used by both the kind and genre filter rows. */
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .background(if (selected) Accent else Surface2, CircleShape)
            .border(1.dp, if (selected) Accent else Stroke, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) AccentInk else TextSecondary,
        )
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Accent,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onAction),
                )
            }
        }
    }
}
