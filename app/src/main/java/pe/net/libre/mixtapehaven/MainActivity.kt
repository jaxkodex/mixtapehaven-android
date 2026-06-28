package pe.net.libre.mixtapehaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.navigation.AppStateViewModel
import pe.net.libre.mixtapehaven.ui.navigation.MixtapeNavHost
import pe.net.libre.mixtapehaven.ui.theme.Bg
import pe.net.libre.mixtapehaven.ui.theme.MixtapeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MixtapeApp() }
    }
}

@Composable
fun MixtapeApp() {
    val appState = appViewModel { AppStateViewModel(it.repository) }
    val startDestination by appState.startDestination.collectAsState()
    MixtapeTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            // Keep the splash background until the session has been restored.
            startDestination?.let { MixtapeNavHost(startDestination = it) }
        }
    }
}
