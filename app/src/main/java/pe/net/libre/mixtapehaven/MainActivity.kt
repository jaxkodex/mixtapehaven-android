package pe.net.libre.mixtapehaven

import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.navigation.NavGraph
import pe.net.libre.mixtapehaven.ui.navigation.Screen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.MixtapeHavenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize dependencies
        val dataStoreManager = DataStoreManager(applicationContext)
        val connectionRepository = ConnectionRepository(dataStoreManager, applicationContext)
        val mediaRepository = MediaRepository(dataStoreManager, applicationContext)
        val onboardingViewModel = OnboardingViewModel(connectionRepository)

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
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}