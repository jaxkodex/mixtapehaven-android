package pe.net.libre.mixtapehaven.data.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for the reachability verdict.
 *
 * The case that motivates all of this: a device with a perfectly good mobile connection and a
 * server that only exists on the home LAN, or behind a VPN that is currently down. Connectivity
 * says "online" and every stream still fails.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerAvailabilityTest {

    private class FakeNetworkMonitor(initial: Boolean = true) : NetworkMonitor {
        val state = MutableStateFlow(initial)
        override val online: Flow<Boolean> = state
        override fun isOnline(): Boolean = state.value
    }

    /** Counts pings so the tests can assert the request is not made needlessly. */
    private class FakePing(var answers: Boolean = true, val delayMs: Long = 0L) {
        var calls = 0
            private set

        suspend operator fun invoke(): Boolean {
            calls++
            if (delayMs > 0) delay(delayMs)
            return answers
        }
    }

    private fun TestScope.availability(
        monitor: FakeNetworkMonitor = FakeNetworkMonitor(),
        ping: FakePing = FakePing(),
        clock: () -> Long = { 1_000L },
    ) = ServerAvailability(monitor, { ping() }, backgroundScope, clock)

    @Test
    fun `no network is answered without asking the server`() = runTest {
        val monitor = FakeNetworkMonitor(initial = false)
        val ping = FakePing()
        val availability = availability(monitor, ping)

        assertFalse(availability.check())
        assertEquals(0, ping.calls)
    }

    /** The LAN-only / VPN case: connectivity is fine, the server still isn't there. */
    @Test
    fun `an online device with an unreachable server is not reachable`() = runTest {
        val ping = FakePing(answers = false)
        val availability = availability(ping = ping)

        assertFalse(availability.check())
        assertFalse(availability.reachable.value)
        assertEquals(1, ping.calls)
    }

    @Test
    fun `a recent success stands in for a ping`() = runTest {
        val ping = FakePing()
        val availability = availability(ping = ping)

        availability.report(success = true)

        assertTrue(availability.check())
        assertEquals(0, ping.calls)
    }

    @Test
    fun `a stale success is confirmed with a ping`() = runTest {
        var nowMs = 1_000L
        val ping = FakePing()
        val availability = availability(ping = ping, clock = { nowMs })

        availability.report(success = true)
        nowMs += 120_000L

        assertTrue(availability.check())
        assertEquals(1, ping.calls)
    }

    /** A believed-down server must not stay down once the VPN is back: every check re-asks. */
    @Test
    fun `a server that comes back is picked up on the next check`() = runTest {
        val ping = FakePing(answers = false)
        val availability = availability(ping = ping)
        availability.report(success = false)
        assertFalse(availability.check())

        ping.answers = true

        assertTrue(availability.check())
        assertTrue(availability.reachable.value)
    }

    @Test
    fun `concurrent checks share one ping`() = runTest {
        val ping = FakePing(delayMs = 500)
        val availability = availability(ping = ping)

        val results = coroutineScope {
            val first = async { availability.check() }
            val second = async { availability.check() }
            listOf(first.await(), second.await())
        }

        assertEquals(listOf(true, true), results)
        assertEquals(1, ping.calls)
    }

    /**
     * A refusal leaves no fresh success behind, so callers that merely serialized would each re-ping
     * — the repeat-tap case, where four taps on a dimmed card used to mean four timeouts in a row.
     */
    @Test
    fun `concurrent checks share one failing ping`() = runTest {
        val ping = FakePing(answers = false, delayMs = 500)
        val availability = availability(ping = ping)

        val results = coroutineScope {
            val first = async { availability.check() }
            val second = async { availability.check() }
            val third = async { availability.check() }
            listOf(first.await(), second.await(), third.await())
        }

        assertEquals(listOf(false, false, false), results)
        assertEquals(1, ping.calls)
    }

    /** Sharing must not outlive the request: the tap after it gets a current answer, not a cached one. */
    @Test
    fun `a later check starts a new ping`() = runTest {
        val ping = FakePing(answers = false)
        val availability = availability(ping = ping)

        assertFalse(availability.check())
        assertFalse(availability.check())

        assertEquals(2, ping.calls)
    }

    /** A caller that gives up must not take the answer away from the ones still waiting. */
    @Test
    fun `abandoning a check leaves the shared ping running`() = runTest {
        val ping = FakePing(delayMs = 500)
        val availability = availability(ping = ping)

        val joined = coroutineScope {
            val abandoned = async { availability.check() }
            val waiting = async { availability.check() }
            runCurrent()
            abandoned.cancel()
            waiting.await()
        }

        assertTrue(joined)
        assertEquals(1, ping.calls)
    }

    @Test
    fun `losing the network marks the server unreachable`() = runTest {
        val monitor = FakeNetworkMonitor()
        val availability = availability(monitor)
        availability.report(success = true)

        monitor.state.value = false
        runCurrent()

        assertFalse(availability.reachable.value)
    }

    /** Regaining a network proves nothing about a VPN-gated server, so it is confirmed. */
    @Test
    fun `a network coming back is confirmed rather than assumed`() = runTest {
        val monitor = FakeNetworkMonitor()
        val ping = FakePing(answers = false)
        val availability = availability(monitor, ping)
        monitor.state.value = false
        runCurrent()

        monitor.state.value = true
        runCurrent()

        assertEquals(1, ping.calls)
        assertFalse(availability.reachable.value)
    }
}
