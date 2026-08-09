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
import pe.net.libre.mixtapehaven.ui.navigation.NoPendingRoute
import pe.net.libre.mixtapehaven.ui.navigation.Routes
import pe.net.libre.mixtapehaven.ui.theme.Bg
import pe.net.libre.mixtapehaven.ui.theme.MixtapeTheme

class MainActivity : ComponentActivity() {

    /** Route requested by the intent that opened (or re-opened) the app; null once consumed. */
    private val _pendingRoute = MutableStateFlow<String?>(null)
    private val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch. A non-null savedInstanceState means the activity is being
        // recreated (rotation, or Recents after a process kill), and the launch intent the system
        // replays still carries the extra — honouring it would yank the user back to Now Playing.
        if (savedInstanceState == null) readPendingRoute(intent)
        setContent {
            MixtapeApp(
                pendingRoute = pendingRoute,
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
        consumeNowPlayingRequest(intent)?.let { _pendingRoute.value = it }
    }

    companion object {
        /** Set by the playback notification's content intent to open Now Playing on arrival. */
        const val EXTRA_OPEN_NOW_PLAYING = "pe.net.libre.mixtapehaven.OPEN_NOW_PLAYING"
    }
}

/**
 * The route [intent] asks to open, or null if it carries no request. Reading is destructive: the
 * extra is removed, because [MainActivity.setIntent] retains the intent and the system replays it
 * whenever the activity is recreated from it — a second read must not re-navigate the user.
 */
internal fun consumeNowPlayingRequest(intent: Intent): String? {
    if (!intent.getBooleanExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, false)) return null
    intent.removeExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING)
    return Routes.NOW_PLAYING
}

@Composable
fun MixtapeApp(
    pendingRoute: StateFlow<String?> = NoPendingRoute,
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
