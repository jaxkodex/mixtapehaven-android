package pe.net.libre.mixtapehaven.ui.screens.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.net.libre.mixtapehaven.data.jellyfin.VideoFilter
import pe.net.libre.mixtapehaven.data.jellyfin.VideoLibrarySource
import pe.net.libre.mixtapehaven.data.jellyfin.VideoPage
import pe.net.libre.mixtapehaven.data.jellyfin.VideoSort
import pe.net.libre.mixtapehaven.model.VideoItem
import pe.net.libre.mixtapehaven.model.VideoKind

/** Unit coverage for the library grid's paging and facet-reset state machine. */
@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun video(id: String) = VideoItem(id = id, title = id, kind = VideoKind.MOVIE)

    /**
     * Serves [total] items in pages, recording every request. [failAfter] makes every call past
     * that many requests throw, for the load-more failure path.
     */
    private class FakeSource(
        private val total: Int,
        private val pageSize: Int = 3,
        private val failAfter: Int = Int.MAX_VALUE,
        private val genreList: List<String> = emptyList(),
        private val itemsOverride: ((Int) -> List<VideoItem>)? = null,
    ) : VideoLibrarySource {
        val requests = mutableListOf<Triple<VideoFilter, String?, Int>>()

        override suspend fun videoLibrary(
            filter: VideoFilter,
            genre: String?,
            sort: VideoSort,
            startIndex: Int,
            limit: Int,
        ): VideoPage {
            requests += Triple(filter, genre, startIndex)
            if (requests.size > failAfter) error("network down")
            val items = itemsOverride?.invoke(startIndex)
                ?: (startIndex until minOf(startIndex + pageSize, total))
                    .map { VideoItem(id = "v$it", title = "v$it", kind = VideoKind.MOVIE) }
            return VideoPage(items = items, totalCount = total)
        }

        override suspend fun videoGenres(limit: Int): List<String> = genreList

        override suspend fun searchVideos(query: String, limit: Int): List<VideoItem> = emptyList()
    }

    @Test
    fun `first page loads and reports more remaining`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 7))
        advanceUntilIdle()

        assertEquals(listOf("v0", "v1", "v2"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertFalse(vm.state.value.endReached)
    }

    @Test
    fun `load more appends the next page`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 7))
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("v0", "v1", "v2", "v3", "v4", "v5"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `paging stops once the whole set is loaded`() = runTest {
        val source = FakeSource(total = 4)
        val vm = VideoLibraryViewModel(source)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertTrue(vm.state.value.endReached)

        val requestsBefore = source.requests.size
        vm.loadMore()
        advanceUntilIdle()

        assertEquals(requestsBefore, source.requests.size)
    }

    /** The crash guard: a shifting library can serve the boundary item in two consecutive pages. */
    @Test
    fun `an item repeated across pages is appended once`() = runTest {
        val source = FakeSource(
            total = 6,
            itemsOverride = { startIndex ->
                if (startIndex == 0) listOf(video("a"), video("b"), video("c"))
                else listOf(video("c"), video("d"), video("e"))
            },
        )
        val vm = VideoLibraryViewModel(source)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c", "d", "e"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `changing the filter refetches from the start`() = runTest {
        val source = FakeSource(total = 9)
        val vm = VideoLibraryViewModel(source)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        vm.setFilter(VideoFilter.SERIES)
        advanceUntilIdle()

        assertEquals(VideoFilter.SERIES to 0, source.requests.last().first to source.requests.last().third)
        assertEquals(3, vm.state.value.items.size)
    }

    @Test
    fun `a facet change bumps the generation so the grid scrolls back to the top`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 9))
        advanceUntilIdle()
        val before = vm.state.value.facetGeneration

        vm.setSort(VideoSort.TITLE)
        advanceUntilIdle()

        assertTrue(vm.state.value.facetGeneration > before)
    }

    @Test
    fun `selecting the active genre clears it`() = runTest {
        val source = FakeSource(total = 9)
        val vm = VideoLibraryViewModel(source)
        advanceUntilIdle()

        vm.setGenre("Drama")
        advanceUntilIdle()
        assertEquals("Drama", vm.state.value.genre)

        vm.setGenre("Drama")
        advanceUntilIdle()
        assertNull(vm.state.value.genre)
        assertNull(source.requests.last().second)
    }

    @Test
    fun `a failed first load surfaces an error`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 9, failAfter = 0))
        advanceUntilIdle()

        assertEquals(emptyList<VideoItem>(), vm.state.value.items)
        assertEquals("network down", vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    /** A failed "load more" must keep what is already on screen rather than blanking the grid. */
    @Test
    fun `a failed load more keeps the loaded items and shows no error`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 9, failAfter = 1))
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("v0", "v1", "v2"), vm.state.value.items.map { it.id })
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.loadingMore)
    }

    @Test
    fun `genres load into state`() = runTest {
        val vm = VideoLibraryViewModel(FakeSource(total = 1, genreList = listOf("Drama", "Sci-Fi")))
        advanceUntilIdle()

        assertEquals(listOf("Drama", "Sci-Fi"), vm.state.value.genres)
    }

    @Test
    fun `retry after a failure reloads`() = runTest {
        val source = FakeSource(total = 9, failAfter = 0)
        val vm = VideoLibraryViewModel(source)
        advanceUntilIdle()
        val requestsBefore = source.requests.size

        vm.retry()
        advanceUntilIdle()

        assertTrue(source.requests.size > requestsBefore)
    }
}
