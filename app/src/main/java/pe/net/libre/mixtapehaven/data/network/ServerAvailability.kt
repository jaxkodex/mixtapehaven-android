package pe.net.libre.mixtapehaven.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Whether the *media server* can be reached — the question the UI actually needs before offering a
 * title it would have to stream.
 *
 * Device connectivity is only the first gate. A Jellyfin box published on the LAN only, or behind a
 * VPN that is currently down, is unreachable from a perfectly healthy mobile connection, and a
 * "you're online" signal would go on promising playback that cannot happen. So [reachable] folds
 * three things together: connectivity (a fast, certain no), the outcome of real requests the app
 * already makes ([report]), and a cheap ping when neither has a fresh answer ([check]).
 *
 * [reachable] starts optimistic: nothing is dimmed on a cold start until something actually fails.
 */
class ServerAvailability(
    private val networkMonitor: NetworkMonitor,
    private val ping: suspend () -> Boolean,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _reachable = MutableStateFlow(true)

    /** Last known verdict, for painting the UI. [check] is the authority before refusing an action. */
    val reachable: StateFlow<Boolean> = _reachable.asStateFlow()

    /**
     * When the server last answered, so a recent success can stand in for a fresh ping. Null until
     * something has actually succeeded — the optimistic starting value of [reachable] is a painting
     * decision, not evidence, and must not satisfy [check].
     */
    @Volatile
    private var lastSuccessAtMs: Long? = null

    /** Serializes pings so a burst of taps costs one request, not one each. */
    private val pingLock = Mutex()

    init {
        scope.launch {
            networkMonitor.online.collectIndexed { index, online ->
                when {
                    // Losing the network is conclusive.
                    !online -> markUnreachable()
                    // Regaining it says nothing about the server (the VPN may still be down), so
                    // confirm rather than assume. Only on a *change* though: pinging on the first
                    // emission would duplicate the request the screen that just opened is making.
                    index > 0 -> check()
                }
            }
        }
    }

    /**
     * Whether the device has a network at all, which separates "you're offline" from "your server
     * isn't answering" when telling the user why something cannot play.
     */
    fun hasNetwork(): Boolean = networkMonitor.isOnline()

    /**
     * Fold in the result of a request the app made anyway. Cheaper and more current than a ping:
     * a screen that just loaded has already proven the server answers.
     */
    fun report(success: Boolean) {
        if (success) markReachable() else markUnreachable()
    }

    /**
     * The authoritative answer, used before refusing to play something.
     *
     * Short-circuits on no connectivity, trusts a recent success, and otherwise pings with a tight
     * timeout — a stalled TCP connect to an unroutable LAN address must not hang a tap.
     */
    suspend fun check(): Boolean = when {
        !networkMonitor.isOnline() -> {
            markUnreachable()
            false
        }
        hasFreshSuccess() -> true
        // A ping may have landed while this call waited for the lock, hence the second look —
        // short-circuiting means the ping is skipped when it did.
        else -> pingLock.withLock { hasFreshSuccess() || pingNow() }
    }

    /** One ping, recorded either way. Callers hold [pingLock]. */
    private suspend fun pingNow(): Boolean {
        // Running out of the budget (null) is as good as a refusal: the server did not answer.
        val answered = withTimeoutOrNull(PING_TIMEOUT_MS) { runCatching { ping() }.getOrDefault(false) } ?: false
        if (answered) markReachable() else markUnreachable()
        return answered
    }

    /** True when the server answered recently enough that re-asking would be waste. */
    private fun hasFreshSuccess(): Boolean {
        val last = lastSuccessAtMs ?: return false
        return _reachable.value && now() - last < FRESH_MS
    }

    private fun markReachable() {
        lastSuccessAtMs = now()
        _reachable.value = true
    }

    private fun markUnreachable() {
        _reachable.value = false
    }

    private companion object {
        /** How long a successful exchange stands in for a ping. */
        const val FRESH_MS = 30_000L

        /** Ping budget. Long enough for a slow LAN, short enough that a tap still feels answered. */
        const val PING_TIMEOUT_MS = 3_000L
    }
}
