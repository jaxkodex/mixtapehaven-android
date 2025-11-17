package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.HomeScreen
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
    data object NowPlaying : Screen("now_playing")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel,
    mediaRepository: MediaRepository,
    connectionRepository: ConnectionRepository,
    playbackManager: PlaybackManager,
    startDestination: String = Screen.Onboarding.route
) {
    val coroutineScope = rememberCoroutineScope()

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
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
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
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
                }
            )
        }

        composable(Screen.AllSongs.route) {
            AllSongsScreen(
                mediaRepository = mediaRepository,
                playbackManager = playbackManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Screen.NowPlaying.route)
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
    }
}
