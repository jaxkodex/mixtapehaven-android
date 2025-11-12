package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllAlbumsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllArtistsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllSongsScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.troubleshoot.TroubleshootScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Troubleshoot : Screen("troubleshoot")
    data object AllAlbums : Screen("all_albums")
    data object AllArtists : Screen("all_artists")
    data object AllSongs : Screen("all_songs")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel,
    mediaRepository: MediaRepository,
    connectionRepository: ConnectionRepository,
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
                onNavigateToAllAlbums = {
                    navController.navigate(Screen.AllAlbums.route)
                },
                onNavigateToAllArtists = {
                    navController.navigate(Screen.AllArtists.route)
                },
                onNavigateToAllSongs = {
                    navController.navigate(Screen.AllSongs.route)
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
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AllArtists.route) {
            AllArtistsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AllSongs.route) {
            AllSongsScreen(
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
    }
}
