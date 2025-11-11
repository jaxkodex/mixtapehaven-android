package pe.net.libre.mixtapehaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import pe.net.libre.mixtapehaven.data.preferences.DataStoreManager
import pe.net.libre.mixtapehaven.data.repository.ConnectionRepository
import pe.net.libre.mixtapehaven.data.repository.MediaRepository
import pe.net.libre.mixtapehaven.ui.navigation.NavGraph
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
        val mediaRepository = MediaRepository(dataStoreManager)
        val onboardingViewModel = OnboardingViewModel(connectionRepository)

        setContent {
            MixtapeHavenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepSpaceBlack
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        onboardingViewModel = onboardingViewModel,
                        mediaRepository = mediaRepository
                    )
                }
            }
        }
    }
}