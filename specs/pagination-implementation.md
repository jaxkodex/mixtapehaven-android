# Pagination Implementation Plan

**Created:** 2025-11-12
**Feature:** Infinite scroll pagination for detail pages
**Branch:** `claude/add-section-details-pages-011CV4cj38BDLBJ3sg2xBEA3`

---

## Overview

Implement infinite scroll pagination for all three detail pages (Albums, Artists, Songs) to allow users to browse their entire media library beyond the initial limits.

### Current Limitations
- All Albums: 50 items max
- All Artists: 100 items max
- All Songs: 100 items max

### With Pagination
- Users can scroll to load more items automatically
- Page size: 50 items per page for all screens
- Loads next page when user scrolls near bottom (5 items before end)

---

## Technical Implementation

### 1. MediaRepository Updates

Add `startIndex` parameter to pagination methods:

```kotlin
suspend fun getAllAlbums(limit: Int = 50, startIndex: Int = 0): Result<List<Album>>
suspend fun getAllArtists(limit: Int = 50, startIndex: Int = 0): Result<List<Artist>>
suspend fun getAllSongs(limit: Int = 50, startIndex: Int = 0): Result<List<Song>>
```

These methods already use `JellyfinApiService.getItems()` which supports `startIndex` parameter.

---

### 2. ViewModel State Changes

#### AllAlbumsUiState
```kotlin
data class AllAlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,  // NEW
    val hasMorePages: Boolean = true      // NEW
)
```

#### AllArtistsUiState
```kotlin
data class AllArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val groupedArtists: Map<Char, List<Artist>> = emptyMap(),
    val isLoadingMore: Boolean = false,  // NEW
    val hasMorePages: Boolean = true      // NEW
)
```

#### AllSongsUiState
```kotlin
data class AllSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,  // NEW
    val hasMorePages: Boolean = true      // NEW
)
```

---

### 3. ViewModel Implementation

Each ViewModel needs:

1. **PAGE_SIZE constant** = 50
2. **loadMore() function** to fetch next page
3. **Update loadInitial()** to reset pagination state
4. **Update refresh()** to reset pagination state

#### Example Pattern (AllAlbumsViewModel):

```kotlin
class AllAlbumsViewModel(
    private val mediaRepository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllAlbumsUiState())
    val uiState: StateFlow<AllAlbumsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                hasMorePages = true
            )

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { albums ->
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        isLoading = false,
                        errorMessage = null,
                        hasMorePages = albums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load albums"
                    )
                }
            )
        }
    }

    fun loadMore() {
        // Don't load if already loading or no more pages
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val currentSize = _uiState.value.albums.size
            val startIndex = currentSize

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = startIndex).fold(
                onSuccess = { newAlbums ->
                    _uiState.value = _uiState.value.copy(
                        albums = _uiState.value.albums + newAlbums,
                        isLoadingMore = false,
                        hasMorePages = newAlbums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Failed to load more albums"
                    )
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                errorMessage = null
            )

            mediaRepository.getAllAlbums(limit = PAGE_SIZE, startIndex = 0).fold(
                onSuccess = { albums ->
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        isRefreshing = false,
                        errorMessage = null,
                        hasMorePages = albums.size >= PAGE_SIZE
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to refresh albums"
                    )
                }
            )
        }
    }
}
```

---

### 4. Screen Implementation

Each screen needs:

1. **LazyListState** or **LazyGridState** to track scroll position
2. **LaunchedEffect** to detect when near bottom
3. **Loading indicator** at bottom when `isLoadingMore = true`

#### Pattern for LazyColumn (Artists, Songs):

```kotlin
val listState = rememberLazyListState()

LaunchedEffect(listState) {
    snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
    }.collect { lastVisibleIndex ->
        if (lastVisibleIndex != null) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleIndex >= totalItems - 5 && !uiState.isLoadingMore) {
                viewModel.loadMore()
            }
        }
    }
}

LazyColumn(state = listState) {
    items(uiState.items) { item -> /* ... */ }

    if (uiState.isLoadingMore) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberNeonBlue)
            }
        }
    }
}
```

#### Pattern for LazyVerticalGrid (Albums):

```kotlin
val gridState = rememberLazyGridState()

LaunchedEffect(gridState) {
    snapshotFlow {
        gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
    }.collect { lastVisibleIndex ->
        if (lastVisibleIndex != null) {
            val totalItems = gridState.layoutInfo.totalItemsCount
            if (lastVisibleIndex >= totalItems - 5 && !uiState.isLoadingMore) {
                viewModel.loadMore()
            }
        }
    }
}

LazyVerticalGrid(state = gridState) {
    items(uiState.albums) { album -> /* ... */ }

    if (uiState.isLoadingMore) {
        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberNeonBlue)
            }
        }
    }
}
```

---

## Special Consideration: AllArtistsViewModel

The AllArtistsViewModel has special grouping logic for the alphabet index. When loading more artists:

```kotlin
fun loadMore() {
    if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoadingMore = true)

        val currentSize = _uiState.value.artists.size

        mediaRepository.getAllArtists(limit = PAGE_SIZE, startIndex = currentSize).fold(
            onSuccess = { newArtists ->
                val allArtists = (_uiState.value.artists + newArtists)
                    .sortedBy { it.name.uppercase() }

                val groupedArtists = allArtists
                    .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                _uiState.value = _uiState.value.copy(
                    artists = allArtists,
                    groupedArtists = groupedArtists,
                    isLoadingMore = false,
                    hasMorePages = newArtists.size >= PAGE_SIZE
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = error.message ?: "Failed to load more artists"
                )
            }
        )
    }
}
```

---

## UI/UX Details

### Loading Indicator Position
- **Albums**: Bottom of grid, spans 2 columns
- **Artists**: Bottom of list, below last artist
- **Songs**: Bottom of list, below last song

### Loading Trigger
- Trigger when user scrolls to **5 items before the end**
- This ensures smooth loading before user reaches actual bottom

### Loading More State
- Show `CircularProgressIndicator` at bottom while loading
- Don't block user interaction
- If error occurs while loading more, show error message (don't clear existing items)

### End of List
- When `hasMorePages = false`, stop showing loading indicator
- No special "end of list" message needed

---

## Error Handling

### Initial Load Error
- Clear all items
- Show error message with retry button
- Same as current implementation

### Load More Error
- Keep existing items visible
- Show error message at bottom or as toast
- Allow user to retry by scrolling again

---

## Testing Checklist

- [ ] Albums page loads 50 albums initially
- [ ] Scrolling to bottom loads next 50 albums
- [ ] Albums pagination continues until all albums loaded
- [ ] Artists page loads 50 artists initially
- [ ] Scrolling to bottom loads next 50 artists
- [ ] Artists remain alphabetically sorted after pagination
- [ ] Alphabet index works with paginated artists
- [ ] Songs page loads 50 songs initially
- [ ] Scrolling to bottom loads next 50 songs
- [ ] Track numbers remain sequential after pagination
- [ ] Loading indicators show at bottom while loading more
- [ ] Pull-to-refresh resets pagination to page 1
- [ ] Error handling works for both initial and pagination loads
- [ ] No duplicate items after pagination
- [ ] Performance remains smooth with 500+ items

---

## Performance Considerations

### Memory Management
- With pagination, users can load 1000+ items
- Jetpack Compose LazyColumn/LazyVerticalGrid handles recycling automatically
- No additional memory management needed

### Scroll Performance
- LazyList recycling ensures smooth scrolling even with many items
- Image loading via Coil is already optimized
- No performance degradation expected

### Network Efficiency
- Loading 50 items at a time balances network requests and user experience
- Jellyfin API is efficient for these request sizes

---

## Implementation Order

1. ✅ Create pagination plan document
2. ⏳ Update MediaRepository with startIndex parameters
3. ⏳ Update AllAlbumsViewModel with pagination
4. ⏳ Update AllAlbumsScreen with scroll detection
5. ⏳ Update AllArtistsViewModel with pagination
6. ⏳ Update AllArtistsScreen with scroll detection
7. ⏳ Update AllSongsViewModel with pagination
8. ⏳ Update AllSongsScreen with scroll detection
9. ⏳ Test with large libraries
10. ⏳ Commit and push

---

## Summary

This pagination implementation enables users to browse their entire Jellyfin music library without limits. The infinite scroll pattern provides a seamless experience, automatically loading more content as users scroll. All existing features (pull-to-refresh, error handling, empty states) remain functional.
