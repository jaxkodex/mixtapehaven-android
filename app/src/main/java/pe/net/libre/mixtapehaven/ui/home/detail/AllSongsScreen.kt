package pe.net.libre.mixtapehaven.ui.home.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.net.libre.mixtapehaven.ui.home.mockPopularSongs
import pe.net.libre.mixtapehaven.ui.home.components.SongListItem
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSongsScreen(
    onNavigateBack: () -> Unit,
    onSongClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Songs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LunarWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberNeonBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyberNeonBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack
                )
            )
        },
        containerColor = DeepSpaceBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            itemsIndexed(mockPopularSongs) { index, song ->
                SongListItem(
                    song = song,
                    trackNumber = index + 1,
                    onClick = { onSongClick(song.id) }
                )
            }
        }
    }
}
