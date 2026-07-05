package pe.net.libre.mixtapehaven.data.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-scoped, in-memory ring buffer of recent diagnostic events. Lets a user export a
 * recent-activity log from Settings so failures that are otherwise silent — e.g. a swallowed
 * Random Walk error, or a queue that resolves nothing playable — can be reported without a USB
 * cable or `adb`. Bounded so it never grows without limit; cleared when the process dies.
 */
class DiagnosticsLog(private val capacity: Int = DEFAULT_CAPACITY) {

    private data class Entry(val timestampMs: Long, val tag: String, val message: String)

    // Oldest first; capped at [capacity]. Guarded by [lock] as events arrive from multiple threads
    // (UI coroutines, the media3 controller callback, refill jobs).
    private val entries = ArrayDeque<Entry>()
    private val lock = Any()

    /** Record a timestamped [message] under [tag]. Thread-safe; the oldest entry drops past capacity. */
    fun log(tag: String, message: String) {
        synchronized(lock) {
            entries.addLast(Entry(System.currentTimeMillis(), tag, message))
            while (entries.size > capacity) entries.removeFirst()
        }
    }

    /** A newest-last, plain-text snapshot of the buffer, one entry per line, suitable for sharing. */
    fun snapshot(): String = synchronized(lock) {
        if (entries.isEmpty()) return "No diagnostic events recorded yet."
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        entries.joinToString("\n") { "${format.format(Date(it.timestampMs))}  [${it.tag}]  ${it.message}" }
    }

    private companion object {
        const val DEFAULT_CAPACITY = 200
    }
}
