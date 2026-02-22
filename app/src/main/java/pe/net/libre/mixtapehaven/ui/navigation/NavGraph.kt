package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import pe.net.libre.mixtapehaven.data.util.NetworkUtil
import pe.net.libre.mixtapehaven.ui.artist.ArtistDetailScreen
import pe.net.libre.mixtapehaven.ui.downloads.DownloadsScreen
import pe.net.libre.mixtapehaven.ui.downloads.DownloadsViewModel
import pe.net.libre.mixtapehaven.ui.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.home.components.NowPlayingBar
import pe.net.libre.mixtapehaven.ui.home.detail.AllAlbumsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllArtistsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllPlaylistsScreen
import pe.net.libre.mixtapehaven.ui.home.detail.AllSongsScreen
import pe.net.libre.mixtapehaven.ui.nowplaying.NowPlayingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.playlist.PlaylistDetailScreen
import pe.net.libre.mixtapehaven.ui.search.SearchScreen
import pe.net.libre.mixtapehaven.ui.settings.SettingsScreen
import pe.net.libre.mixtapehaven.ui.settings.SettingsViewModel
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
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

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenter: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Browse", Screen.AllAlbums.route, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavItem("Search", Screen.Search.route, Icons.Filled.Search, Icons.Outlined.Search, isCenter = true),
    BottomNavItem("Library", Screen.AllPlaylists.route, Icons.Filled.GridView, Icons.Outlined.GridView),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

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

    // Determine visibility
    val shouldShowBottomBar = when {
        currentRoute == null -> false
        currentRoute == Screen.Onboarding.route -> false
        currentRoute == Screen.NowPlaying.route -> false
        else -> true
    }

    val shouldShowNowPlayingBar = shouldShowBottomBar

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DeepSpaceBlack,
            bottomBar = {
                if (shouldShowBottomBar) {
                    NavigationBar(
                        containerColor = DeepSpaceBlack,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route

                            if (item.isCenter) {
                                // Center Search button - elevated FAB-like
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navigateToBottomNavRoute(navController, item.route)
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFF5C6BC0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.selectedIcon,
                                                contentDescription = item.label,
                                                tint = DeepSpaceBlack,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    label = null,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            } else {
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navigateToBottomNavRoute(navController, item.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) CyberNeonBlue else LunarWhite.copy(alpha = 0.5f)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) CyberNeonBlue else LunarWhite.copy(alpha = 0.5f)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
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
                val context = LocalContext.current
                val networkConnectivityProvider = remember { NetworkUtil.createProvider(context) }
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    mediaRepository = mediaRepository,
                    playbackManager = playbackManager,
                    offlineRepository = offlineRepository,
                    dataStoreManager = dataStoreManager,
                    networkConnectivityProvider = networkConnectivityProvider,
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
                    mediaRepository = mediaRepository,
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
    }

        // Floating NowPlayingBar overlay - sits above bottom nav with transparent background
        if (shouldShowNowPlayingBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 80.dp) // offset above the bottom nav bar
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                NowPlayingBar(
                    playbackState = playbackState,
                    onPlayPauseClick = { playbackManager.togglePlayPause() },
                    onBarClick = { navController.navigate(Screen.NowPlaying.route) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun navigateToBottomNavRoute(navController: NavHostController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination to avoid building up a large stack
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}
