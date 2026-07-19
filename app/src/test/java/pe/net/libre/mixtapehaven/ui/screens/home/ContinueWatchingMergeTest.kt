package pe.net.libre.mixtapehaven.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for merging the local and server Continue watching lists into one rail. */
class ContinueWatchingMergeTest {

    private fun item(id: String, positionMs: Long, lastPlayedAtMs: Long, title: String = id) = VideoItem(
        id = id,
        title = title,
        kind = VideoKind.MOVIE,
        runtimeMs = 3_600_000L,
        resumePositionMs = positionMs,
        lastPlayedAtMs = lastPlayedAtMs,
    )

    @Test
    fun `empty sides merge to nothing`() {
        assertTrue(mergeContinueWatching(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `local-only titles survive so the rail works offline`() {
        val merged = mergeContinueWatching(listOf(item("a", 60_000, 100)), emptyList())

        assertEquals(listOf("a"), merged.map { it.id })
    }

    @Test
    fun `titles are ordered by most recently watched`() {
        val merged = mergeContinueWatching(
            local = listOf(item("a", 60_000, 100)),
            server = listOf(item("b", 60_000, 300), item("c", 60_000, 200)),
        )

        assertEquals(listOf("b", "c", "a"), merged.map { it.id })
    }

    @Test
    fun `a title on both sides appears once`() {
        val merged = mergeContinueWatching(
            local = listOf(item("a", 60_000, 100)),
            server = listOf(item("a", 90_000, 200)),
        )

        assertEquals(1, merged.size)
    }

    @Test
    fun `the fresher record wins for a title on both sides`() {
        val merged = mergeContinueWatching(
            local = listOf(item("a", 600_000, 500, title = "local")),
            server = listOf(item("a", 90_000, 200, title = "server")),
        )

        assertEquals("local", merged.single().title)
        assertEquals(600_000L, merged.single().resumePositionMs)
    }

    /** A finished title reported with a zeroed position must not linger in the rail. */
    @Test
    fun `titles with no position are dropped`() {
        val merged = mergeContinueWatching(
            local = listOf(item("a", 0, 100)),
            server = listOf(item("b", 60_000, 200)),
        )

        assertEquals(listOf("b"), merged.map { it.id })
    }

    @Test
    fun `the rail is capped`() {
        val many = (1..40).map { item("v$it", 60_000, it.toLong()) }

        assertEquals(12, mergeContinueWatching(many, emptyList()).size)
    }
}
