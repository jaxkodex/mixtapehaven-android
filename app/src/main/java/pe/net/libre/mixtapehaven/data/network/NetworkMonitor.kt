package pe.net.libre.mixtapehaven.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device currently has a network at all.
 *
 * This answers "is there any point trying the server", which is what the UI needs to stop offering
 * titles it cannot stream. It deliberately does not promise the server is reachable — a Jellyfin
 * box can be down on a perfectly good connection — so callers still handle request failures.
 */
interface NetworkMonitor {
    /** Emits the current state immediately, then on every connectivity change. */
    val online: Flow<Boolean>

    /** The state right now, for one-shot decisions (e.g. what a tap should do). */
    fun isOnline(): Boolean
}

/**
 * [NetworkMonitor] backed by [ConnectivityManager].
 *
 * Capability is [NetworkCapabilities.NET_CAPABILITY_INTERNET] *without* requiring
 * `NET_CAPABILITY_VALIDATED`: Mixtape Haven usually talks to a self-hosted server on the same LAN,
 * and a Wi-Fi network with no route to the wider internet is never validated — treating that as
 * offline would hide titles the server can actually serve.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val appContext = context.applicationContext

    private val connectivityManager: ConnectivityManager?
        get() = appContext.getSystemService(ConnectivityManager::class.java)

    override val online: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            // No connectivity service to subscribe to: assume online so a missing system service
            // cannot lock the user out of streaming.
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }
        // Networks come and go independently, so track the live set rather than a boolean: losing
        // Wi-Fi while on cellular must not report offline.
        val available = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                available -= network
                trySend(available.isNotEmpty())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        // Callbacks only report changes, so seed the current state for collectors that subscribe
        // long after the last one.
        trySend(isOnline())
        awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged().conflate()

    override fun isOnline(): Boolean {
        val manager = connectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
