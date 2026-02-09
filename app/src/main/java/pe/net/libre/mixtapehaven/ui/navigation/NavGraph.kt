package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.data.repository.OfflineRepository
import pe.net.libre.mixtapehaven.ui.artist.ArtistDetailScreen
import pe.net.libre.mixtapehaven.ui.downloads.DownloadsScreen
import pe.net.libre.mixtapehaven.ui.search.SearchScreen
import pe.net.libre.mixtapehaven.ui.downloads.DownloadsViewModel
import pe.net.libre.mixtapehaven.ui.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.settings.SettingsScreen
import pe.net.libre.mixtapehaven.ui.settings.SettingsViewModel
import pe.net.libre.mixtapehaven.ui.home.components.NowPlayingBar
import pe.net.libre.mixtapehaven.ui.home.detail.AllAlbumsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllArtistsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllPlaylistsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllSongsScreen
import pe.net.libre.mixtapehaven.ui.nowplaying.NowPlayingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.playlist.PlaylistDetailScreen
import pe.net.libre.mixtapehaven.ui.troubleshoot.TroubleshootScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Troubleshoot : Screen("troubleshoot")
    data object AllAlbums : Screen("all_albums")
    data object AllArtists : Screen("all_artists")
    data object AllSongs : Screen("all_songs")
    data object AllPlaylists : Screen("all_playlists")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
    data object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String) = "artist_detail/$artistId"
    }
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")
    data object Downloads : Screen("downloads")
    data object Search : Screen("search")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel,
    mediaRepository: MediaRepository,
    connectionRepository: ConnectionRepository,
    playbackManager: PlaybackManager,
    dataStoreManager: DataStoreManager,
    offlineRepository: OfflineRepository,
    startDestination: String = Screen.Onboarding.route
) {
    val coroutineScope = rememberCoroutineScope()

    // Observe current route for conditional visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Collect playback state for persistent bar
    val playbackState by playbackManager.playbackState.collectAsState()

    // Determine if bar should be visible based on route
    val shouldShowNowPlayingBar = when {
        currentRoute == null -> false
        currentRoute == Screen.Onboarding.route -> false
        currentRoute == Screen.NowPlaying.route -> false
        else -> true  // Show on Home, AllSongs, AllAlbums, AllArtists, AllPlaylists, PlaylistDetail, ArtistDetail, Troubleshoot
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToTroubleshoot = {
                    navController.navigate(Screen.Troubleshoot.route)
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                offlineRepository = offlineRepository,
                dataStoreManager = dataStoreManager,
                onNavigateToAllAlbums = {
                    navController.navigate(Screen.AllAlbums.route)
                },
                onNavigateToAllArtists = {
                    navController.navigate(Screen.AllArtists.route)
                },
                onNavigateToAllSongs = {
                    navController.navigate(Screen.AllSongs.route)
                },
                onNavigateToAllPlaylists = {
                    navController.navigate(Screen.AllPlaylists.route)
                },
                onNavigateToPlaylistDetail = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onNavigateToArtistDetail = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onLogout = {
                    coroutineScope.launch {
                        connectionRepository.clearConnection()
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AllAlbums.route) {
            AllAlbumsScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AllArtists.route) {
            AllArtistsScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                }
            )
        }

        composable(Screen.AllSongs.route) {
            AllSongsScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                offlineRepository = offlineRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AllPlaylists.route) {
            AllPlaylistsScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = artistId,
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Troubleshoot.route) {
            TroubleshootScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel = remember {
                SettingsViewModel(dataStoreManager, offlineRepository)
            }
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Downloads.route) {
            val downloadsViewModel = remember {
                DownloadsViewModel(offlineRepository, playbackManager)
            }
            DownloadsScreen(
                viewModel = downloadsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                mediaRepository = mediaRepository,
                offlineRepository = offlineRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToArtistDetail = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
                onNavigateToPlaylistDetail = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                }
            )
        }
    }

        // Persistent NowPlayingBar outside NavHost
        if (shouldShowNowPlayingBar) {
            NowPlayingBar(
                playbackState = playbackState,
                onPlayPauseClick = { playbackManager.togglePlayPause() },
                onBarClick = { navController.navigate(Screen.NowPlaying.route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
