package pe.net.libre.mixtapehaven.data.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for the season bucketing behind the detail screen's season chips. */
class SeasonGroupingTest {

    private fun episode(id: String, season: Int?) =
        VideoItem(id = id, title = id, kind = VideoKind.EPISODE, seasonNumber = season)

    @Test
    fun `episodes group into ascending seasons`() {
        val grouped = groupBySeason(
            listOf(episode("a", 1), episode("b", 2), episode("c", 1)),
        )

        assertEquals(listOf("Season 1", "Season 2"), grouped.map { it.first })
        assertEquals(listOf("a", "c"), grouped[0].second.map { it.id })
    }

    @Test
    fun `episode order within a season is preserved`() {
        val grouped = groupBySeason(listOf(episode("e3", 1), episode("e1", 1), episode("e2", 1)))

        assertEquals(listOf("e3", "e1", "e2"), grouped.single().second.map { it.id })
    }

    /** Season 0 sorts before season 1 numerically; specials must not open the series. */
    @Test
    fun `season zero becomes Specials and sorts last`() {
        val grouped = groupBySeason(listOf(episode("sp", 0), episode("a", 1), episode("b", 2)))

        assertEquals(listOf("Season 1", "Season 2", SPECIALS_LABEL), grouped.map { it.first })
    }

    @Test
    fun `episodes without a season number fall into Specials`() {
        val grouped = groupBySeason(listOf(episode("a", 1), episode("x", null)))

        assertEquals(listOf("Season 1", SPECIALS_LABEL), grouped.map { it.first })
        assertEquals(listOf("x"), grouped[1].second.map { it.id })
    }

    /** A null season and season 0 are the same bucket, not two competing "Specials" rows. */
    @Test
    fun `null and zero seasons share one bucket`() {
        val grouped = groupBySeason(listOf(episode("x", null), episode("sp", 0)))

        assertEquals(1, grouped.size)
        assertEquals(listOf("x", "sp"), grouped.single().second.map { it.id })
    }

    /**
     * Every non-positive season collapses to one group. Two groups would both render as
     * "Specials", and the season chips are keyed by label — a duplicate key crashes the row.
     */
    @Test
    fun `negative zero and null seasons all share one Specials bucket`() {
        val grouped = groupBySeason(
            listOf(episode("neg", -1), episode("zero", 0), episode("null", null), episode("s1", 1)),
        )

        assertEquals(listOf("Season 1", SPECIALS_LABEL), grouped.map { it.first })
        assertEquals(listOf("neg", "zero", "null"), grouped[1].second.map { it.id })
    }

    @Test
    fun `season labels are unique so the chip row can key on them`() {
        val labels = groupBySeason(
            listOf(episode("a", -2), episode("b", 0), episode("c", null), episode("d", 2)),
        ).map { it.first }

        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun `no episodes yields no seasons`() {
        assertEquals(emptyList<Pair<String, List<VideoItem>>>(), groupBySeason(emptyList()))
    }

    @Test
    fun `season label names real seasons and buckets the rest`() {
        assertEquals("Season 3", seasonLabel(3))
        assertEquals(SPECIALS_LABEL, seasonLabel(0))
        assertEquals(SPECIALS_LABEL, seasonLabel(null))
    }
}
