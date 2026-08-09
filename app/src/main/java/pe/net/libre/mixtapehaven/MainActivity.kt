package pe.net.libre.mixtapehaven

import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.net.libre.mixtapehaven.di.appViewModel
import pe.net.libre.mixtapehaven.ui.navigation.AppStateViewModel
import pe.net.libre.mixtapehaven.ui.navigation.MixtapeNavHost
import pe.net.libre.mixtapehaven.ui.navigation.Routes
import pe.net.libre.mixtapehaven.ui.theme.Bg
import pe.net.libre.mixtapehaven.ui.theme.MixtapeTheme

class MainActivity : ComponentActivity() {

    /** Route requested by the intent that opened (or re-opened) the app; null once consumed. */
    private val _pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readPendingRoute(intent)
        setContent {
            MixtapeApp(
                pendingRoute = _pendingRoute.asStateFlow(),
                onRouteConsumed = { _pendingRoute.value = null },
            )
        }
    }

    /**
     * The activity is `singleTask`, so tapping the media notification while the app is already
     * running re-delivers here instead of going through [onCreate].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readPendingRoute(intent)
    }

    private fun readPendingRoute(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false)) {
            _pendingRoute.value = Routes.NOW_PLAYING
        }
    }

    companion object {
        /** Set by the playback notification's content intent to open Now Playing on arrival. */
        const val EXTRA_OPEN_NOW_PLAYING = "pe.net.libre.mixtapehaven.OPEN_NOW_PLAYING"
    }
}

@Composable
fun MixtapeApp(
    pendingRoute: StateFlow<String?> = MutableStateFlow(null),
    onRouteConsumed: () -> Unit = {},
) {
    val appState = appViewModel { AppStateViewModel(it.repository) }
    val startDestination by appState.startDestination.collectAsState()
    MixtapeTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            // Keep the splash background until the session has been restored.
            startDestination?.let {
                MixtapeNavHost(
                    startDestination = it,
                    pendingRoute = pendingRoute,
                    onRouteConsumed = onRouteConsumed,
                )
            }
        }
    }
}
