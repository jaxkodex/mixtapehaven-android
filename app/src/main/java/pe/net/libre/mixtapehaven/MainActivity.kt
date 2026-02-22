package pe.net.libre.mixtapehaven

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.MediaPlaybackService
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.navigation.NavGraph
import pe.net.libre.mixtapehaven.ui.navigation.Screen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.MixtapeHavenTheme

class MainActivity : ComponentActivity() {
    private var mediaPlaybackService: MediaPlaybackService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlaybackService.LocalBinder
            mediaPlaybackService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlaybackService = null
            serviceBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize dependencies
        val dataStoreManager = DataStoreManager(applicationContext)
        val offlineDatabase = pe.net.libre.mixtapehaven.data.local.OfflineDatabase.getInstance(applicationContext)
        val fileDownloader = pe.net.libre.mixtapehaven.data.download.FileDownloader(dataStoreManager, applicationContext)
        val cacheManager = pe.net.libre.mixtapehaven.data.cache.CacheManager(offlineDatabase, dataStoreManager, applicationContext)
        val downloadManager = pe.net.libre.mixtapehaven.data.download.DownloadManager.getInstance(
            applicationContext, offlineDatabase, fileDownloader, dataStoreManager, cacheManager
        )
        val offlineRepository = pe.net.libre.mixtapehaven.data.repository.OfflineRepository(offlineDatabase, downloadManager, cacheManager)
        val connectionRepository = ConnectionRepository(dataStoreManager, applicationContext)
        val mediaRepository = MediaRepository(dataStoreManager, applicationContext)
        val playbackManager = PlaybackManager.getInstance(applicationContext, dataStoreManager, offlineRepository)
        val onboardingViewModel = OnboardingViewModel(connectionRepository)

        // Start and bind to the MediaPlaybackService
        startAndBindService()

        setContent {
            MixtapeHavenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepSpaceBlack
                ) {
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    // Check if user is already logged in
                    LaunchedEffect(Unit) {
                        val accessToken = dataStoreManager.accessToken.first()
                        val serverUrl = dataStoreManager.serverUrl.first()

                        // If we have both token and server URL, user is logged in
                        startDestination = if (!accessToken.isNullOrEmpty() && !serverUrl.isNullOrEmpty()) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }
                    }

                    // Show loading indicator while checking auth status
                    if (startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            onboardingViewModel = onboardingViewModel,
                            mediaRepository = mediaRepository,
                            connectionRepository = connectionRepository,
                            playbackManager = playbackManager,
                            dataStoreManager = dataStoreManager,
                            offlineRepository = offlineRepository,
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }

        // Monitor playback state to start foreground service when music plays
        lifecycleScope.launch {
            playbackManager.playbackState.collect { state ->
                if (state.isPlaying && serviceBound) {
                    mediaPlaybackService?.startForegroundService()
                }
            }
        }
    }

    private fun startAndBindService() {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_START_FOREGROUND
        }
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}