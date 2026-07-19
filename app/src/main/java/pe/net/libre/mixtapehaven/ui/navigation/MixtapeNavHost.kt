package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.di.AppContainer
import pe.net.libre.mixtapehaven.di.appContainer
import pe.net.libre.mixtapehaven.ui.screens.downloads.DownloadsScreen
import pe.net.libre.mixtapehaven.ui.screens.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.screens.login.LoginScreen
import pe.net.libre.mixtapehaven.ui.screens.nowplaying.NowPlayingScreen
import pe.net.libre.mixtapehaven.ui.screens.search.SearchScreen
import pe.net.libre.mixtapehaven.ui.screens.settings.SettingsScreen
import pe.net.libre.mixtapehaven.ui.screens.video.VideoDetailScreen
import pe.net.libre.mixtapehaven.ui.screens.video.VideoLibraryScreen
import pe.net.libre.mixtapehaven.ui.screens.video.VideoPlayerScreen

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
                onOpenVideo = { itemId -> navController.navigate(Routes.videoDetail(itemId)) },
                onResumeVideo = { itemId -> navController.navigate(Routes.videoPlayer(itemId)) },
                onOpenVideoLibrary = { navController.navigate(Routes.VIDEO_LIBRARY) },
            )
        }
        videoDestinations(navController)
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                onOpenVideo = { itemId -> navController.navigate(Routes.videoDetail(itemId)) },
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
                    signOut(container, scope)
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                onBack = { navController.popBackStack() },
                onPlayVideo = { itemId -> navController.navigate(Routes.videoPlayer(itemId)) },
            )
        }
    }
}

/**
 * Stop playback and drop this user's session and local state.
 *
 * Watch positions are cleared too: they are per-user, so without this the next account to sign in
 * would inherit the previous one's Continue watching rail.
 */
private fun signOut(container: AppContainer, scope: CoroutineScope) {
    container.playerController.stop()
    scope.launch {
        container.videoProgressStore.clearAll()
        container.repository.signOut()
    }
}

/** The video library grid, plus the detail and full-screen player keyed by the Jellyfin item id. */
private fun NavGraphBuilder.videoDestinations(navController: NavHostController) {
    composable(Routes.VIDEO_LIBRARY) {
        VideoLibraryScreen(
            onBack = { navController.popBackStack() },
            onOpenVideo = { itemId -> navController.navigate(Routes.videoDetail(itemId)) },
        )
    }
    composable(Routes.VIDEO_DETAIL) { entry ->
        VideoDetailScreen(
            itemId = entry.arguments?.getString("itemId").orEmpty(),
            onBack = { navController.popBackStack() },
            onPlay = { playId -> navController.navigate(Routes.videoPlayer(playId)) },
        )
    }
    composable(Routes.VIDEO_PLAYER) { entry ->
        VideoPlayerScreen(
            itemId = entry.arguments?.getString("itemId").orEmpty(),
            onBack = { navController.popBackStack() },
        )
    }
}
