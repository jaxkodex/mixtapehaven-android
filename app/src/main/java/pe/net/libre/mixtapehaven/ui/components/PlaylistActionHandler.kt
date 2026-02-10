package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.Song
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

/**
 * A reusable component that handles the complete playlist action flow.
 * Manages state, ViewModel, snackbar feedback, and renders all necessary UI.
 *
 * @param mediaRepository The media repository for playlist operations
 * @param playbackManager The playback manager for instant mix and queue operations
 * @param enabled Whether the playlist actions are enabled (false in offline mode)
 * @param onPlaylistChanged Called when a playlist is created or a song is added, so the parent can refresh
 * @param content The content composable that receives the onMoreClick callback
 */
@Composable
fun PlaylistActionHandler(
    mediaRepository: MediaRepository,
    playbackManager: PlaybackManager,
    enabled: Boolean = true,
    onPlaylistChanged: () -> Unit = {},
    onDownloadSong: ((Song) -> Unit)? = null,
    content: @Composable (onSongMoreClick: (Song) -> Unit) -> Unit
) {
    // State management
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var isLoadingInstantMix by remember { mutableStateOf(false) }

    // Coroutine scope for instant mix
    val coroutineScope = rememberCoroutineScope()

    // ViewModel
    val playlistActionViewModel: PlaylistActionViewModel = viewModel {
        PlaylistActionViewModel(mediaRepository)
    }
    val playlistActionState by playlistActionViewModel.uiState.collectAsState()

    // Snackbar for result feedback
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for playlist action results
    LaunchedEffect(playlistActionState.resultMessage) {
        playlistActionState.resultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Notify parent to refresh if the action succeeded (not an error message)
            if (!message.startsWith("Failed")) {
                onPlaylistChanged()
            }
            playlistActionViewModel.clearResult()
        }
    }

    // Provide the callback to the content
    val onSongMoreClick: (Song) -> Unit = { song ->
        if (enabled) {
            selectedSong = song
            showContextMenu = true
        }
    }

    // Handle instant mix
    val onInstantMix: (Song) -> Unit = { song ->
        showContextMenu = false
        coroutineScope.launch {
            isLoadingInstantMix = true
            try {
                mediaRepository.getSongInstantMix(song.id)
                    .onSuccess { songs ->
                        if (songs.isNotEmpty()) {
                            playbackManager.setQueue(songs, 0)
                            snackbarHostState.showSnackbar("Instant mix generated with ${songs.size} songs")
                        } else {
                            snackbarHostState.showSnackbar("No similar songs found")
                        }
                    }
                    .onFailure { error ->
                        snackbarHostState.showSnackbar("Failed to generate instant mix: ${error.message}")
                    }
            } finally {
                isLoadingInstantMix = false
                selectedSong = null
            }
        }
    }

    // Render content with snackbar host
    Box(modifier = Modifier.fillMaxSize()) {
        content(onSongMoreClick)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DeepSpaceBlack,
                    contentColor = LunarWhite,
                    actionColor = CyberNeonBlue
                )
            }
        )
    }

    // Render playlist action UI
    PlaylistActionFlow(
        selectedSong = selectedSong,
        showContextMenu = showContextMenu,
        showAddToPlaylist = showAddToPlaylist,
        showNewPlaylistDialog = showNewPlaylistDialog,
        playlistActionState = playlistActionState,
        isLoadingInstantMix = isLoadingInstantMix,
        onDownloadSong = onDownloadSong,
        onDismissContextMenu = { showContextMenu = false },
        onAddToPlaylist = {
            showContextMenu = false
            playlistActionViewModel.loadPlaylists()
            showAddToPlaylist = true
        },
        onInstantMix = onInstantMix,
        onSelectPlaylist = { playlist ->
            selectedSong?.let { song ->
                playlistActionViewModel.addSongToPlaylist(song.id, playlist.id)
            }
            showAddToPlaylist = false
            selectedSong = null
        },
        onCreateNewPlaylist = {
            showAddToPlaylist = false
            showNewPlaylistDialog = true
        },
        onDismissAddToPlaylist = {
            showAddToPlaylist = false
            selectedSong = null
        },
        onConfirmNewPlaylist = { name ->
            selectedSong?.let { song ->
                playlistActionViewModel.createPlaylistAndAddSong(name, song.id)
            }
            showNewPlaylistDialog = false
            selectedSong = null
        },
        onDismissNewPlaylist = {
            showNewPlaylistDialog = false
            selectedSong = null
        }
    )
}

/**
 * Renders the UI components for the playlist action flow.
 * This is the presentation layer that shows the bottom sheets and dialogs.
 */
@Composable
private fun PlaylistActionFlow(
    selectedSong: Song?,
    showContextMenu: Boolean,
    showAddToPlaylist: Boolean,
    showNewPlaylistDialog: Boolean,
    playlistActionState: PlaylistActionViewModel.UiState,
    isLoadingInstantMix: Boolean,
    onDownloadSong: ((Song) -> Unit)? = null,
    onDismissContextMenu: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInstantMix: (Song) -> Unit,
    onSelectPlaylist: (pe.net.libre.mixtapehaven.ui.home.Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismissAddToPlaylist: () -> Unit,
    onConfirmNewPlaylist: (String) -> Unit,
    onDismissNewPlaylist: () -> Unit
) {
    // 1. Song context menu
    if (showContextMenu && selectedSong != null && !isLoadingInstantMix) {
        SongContextMenuBottomSheet(
            song = selectedSong,
            onDismiss = onDismissContextMenu,
            onAddToPlaylist = { onAddToPlaylist() },
            onInstantMix = { onInstantMix(selectedSong) },
            onDownloadClick = onDownloadSong?.let { download ->
                { song: Song ->
                    onDismissContextMenu()
                    download(song)
                }
            }
        )
    }

    // 2. Playlist picker
    if (showAddToPlaylist && selectedSong != null) {
        AddToPlaylistSheet(
            playlists = playlistActionState.playlists,
            isLoading = playlistActionState.isLoadingPlaylists,
            onSelectPlaylist = onSelectPlaylist,
            onCreateNew = onCreateNewPlaylist,
            onDismiss = onDismissAddToPlaylist
        )
    }

    // 3. New playlist dialog
    if (showNewPlaylistDialog && selectedSong != null) {
        NewPlaylistDialog(
            isCreating = playlistActionState.isCreatingPlaylist,
            onConfirm = onConfirmNewPlaylist,
            onDismiss = onDismissNewPlaylist
        )
    }
}
