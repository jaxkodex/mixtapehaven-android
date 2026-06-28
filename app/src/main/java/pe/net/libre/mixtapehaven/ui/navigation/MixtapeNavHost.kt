package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.di.appContainer
import pe.net.libre.mixtapehaven.ui.screens.downloads.DownloadsScreen
import pe.net.libre.mixtapehaven.ui.screens.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.screens.login.LoginScreen
import pe.net.libre.mixtapehaven.ui.screens.nowplaying.NowPlayingScreen
import pe.net.libre.mixtapehaven.ui.screens.search.SearchScreen
import pe.net.libre.mixtapehaven.ui.screens.settings.SettingsScreen

@Composable
fun MixtapeNavHost(startDestination: String, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val container = appContainer()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onConnect = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
            )
        }
        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onSignOut = {
                    container.playerController.stop()
                    scope.launch { container.repository.signOut() }
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }
    }
}
