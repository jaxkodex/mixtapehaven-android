# Section Details Pages Implementation Plan

**Created:** 2025-11-12
**Feature:** Update "See More" detail pages to load data from Jellyfin API
**Branch:** `claude/add-section-details-pages-011CV4cj38BDLBJ3sg2xBEA3`

---

## Overview

The home screen displays three main sections with "See More" functionality:
1. **Recently Added Albums** → `AllAlbumsScreen`
2. **Top Artists** → `AllArtistsScreen`
3. **Popular Songs** → `AllSongsScreen`

Currently, all three detail screens use mock data. This implementation will update them to load real data from the Jellyfin API using the existing MediaRepository.

---

## Current State Analysis

### Existing Infrastructure
✅ **API Service**: `JellyfinApiService` with endpoints for albums, artists, and songs
✅ **Repository**: `MediaRepository` with methods:
  - `getAllAlbums(limit: Int = 50)`
  - `getAllArtists(limit: Int = 100)`
  - `getAllSongs(limit: Int = 100)`
✅ **Navigation**: Routes defined in `NavGraph.kt`
✅ **UI Components**: All three detail screens implemented with mock data
✅ **Data Models**: `Album`, `Artist`, `Song` models in `MockData.kt`

### Current Limitations
❌ Detail screens use hardcoded mock data
❌ No loading states in detail screens
❌ No error handling in detail screens
❌ No pagination support (all screens load limited data)
❌ No pull-to-refresh functionality

---

## Implementation Plan

### Phase 1: Create ViewModels

#### 1.1 AllAlbumsViewModel
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllAlbumsViewModel.kt`

**Responsibilities**:
- Load all albums from MediaRepository
- Manage UI state (loading, success, error)
- Support for future pagination
- Pull-to-refresh functionality

**UI State**:
```kotlin
data class AllAlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)
```

**API Call**:
- `mediaRepository.getAllAlbums(limit = 50)` (configurable)
- Can be increased based on performance testing

---

#### 1.2 AllArtistsViewModel
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllArtistsViewModel.kt`

**Responsibilities**:
- Load all artists from MediaRepository
- Manage UI state (loading, success, error)
- Support alphabetical grouping for the alphabet index
- Pull-to-refresh functionality

**UI State**:
```kotlin
data class AllArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val groupedArtists: Map<Char, List<Artist>> = emptyMap()
)
```

**API Call**:
- `mediaRepository.getAllArtists(limit = 100)` (configurable)
- Group artists by first letter for alphabet index

---

#### 1.3 AllSongsViewModel
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllSongsViewModel.kt`

**Responsibilities**:
- Load all songs from MediaRepository
- Manage UI state (loading, success, error)
- Support sorting by play count/date played (current API default)
- Pull-to-refresh functionality

**UI State**:
```kotlin
data class AllSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)
```

**API Call**:
- `mediaRepository.getAllSongs(limit = 100)` (configurable)
- Already sorted by PlayCount in the repository

---

### Phase 2: Update Detail Screens

#### 2.1 AllAlbumsScreen Updates
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllAlbumsScreen.kt`

**Changes**:
1. Add ViewModel parameter: `viewModel: AllAlbumsViewModel = viewModel()`
2. Collect UI state: `val uiState by viewModel.uiState.collectAsState()`
3. Replace `mockRecentlyAddedAlbums` with `uiState.albums`
4. Add loading indicator when `uiState.isLoading`
5. Add error message display when `uiState.errorMessage != null`
6. Add pull-to-refresh support using `PullToRefreshBox` (Material 3)
7. Show empty state when no albums found

**UI Flow**:
```
Loading → [CircularProgressIndicator centered]
Success → [2-column LazyVerticalGrid of albums]
Error → [Error message with retry button]
Empty → [Empty state message]
```

---

#### 2.2 AllArtistsScreen Updates
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllArtistsScreen.kt`

**Changes**:
1. Add ViewModel parameter: `viewModel: AllArtistsViewModel = viewModel()`
2. Collect UI state: `val uiState by viewModel.uiState.collectAsState()`
3. Replace `mockAllArtists` with `uiState.artists`
4. Add loading indicator when `uiState.isLoading`
5. Add error message display when `uiState.errorMessage != null`
6. Add pull-to-refresh support
7. Update alphabet index to work with `uiState.groupedArtists`
8. Show empty state when no artists found

**UI Flow**:
```
Loading → [CircularProgressIndicator centered]
Success → [LazyColumn of artists with alphabet index]
Error → [Error message with retry button]
Empty → [Empty state message]
```

---

#### 2.3 AllSongsScreen Updates
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/detail/AllSongsScreen.kt`

**Changes**:
1. Add ViewModel parameter: `viewModel: AllSongsViewModel = viewModel()`
2. Collect UI state: `val uiState by viewModel.uiState.collectAsState()`
3. Replace `mockPopularSongs` with `uiState.songs`
4. Add loading indicator when `uiState.isLoading`
5. Add error message display when `uiState.errorMessage != null`
6. Add pull-to-refresh support
7. Show empty state when no songs found

**UI Flow**:
```
Loading → [CircularProgressIndicator centered]
Success → [LazyColumn of songs with track numbers]
Error → [Error message with retry button]
Empty → [Empty state message]
```

---

### Phase 3: Update Navigation

#### 3.1 NavGraph Updates
**File**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/navigation/NavGraph.kt`

**Changes**:
1. Ensure ViewModels are properly scoped for each composable route
2. No additional changes needed as ViewModels will be created inline in each screen

---

## Technical Specifications

### Data Flow
```
AllAlbumsScreen → AllAlbumsViewModel → MediaRepository → JellyfinApiService
                     ↓
                  UI State
                     ↓
                  Composable
```

### Error Handling
- Network errors: Display user-friendly message with retry option
- Authentication errors: Show error and provide navigation back to login
- Empty results: Show empty state message
- All errors logged for debugging

### Loading States
- Initial load: Full-screen CircularProgressIndicator
- Refresh: Pull-to-refresh indicator at top
- No skeleton screens (keep it simple for now)

### Performance Considerations
- Initial limits: 50 albums, 100 artists, 100 songs
- Can be increased based on testing
- Future optimization: Implement pagination with `StartIndex` parameter
- Images loaded lazily via Coil (already implemented in components)

---

## API Endpoints Used

### Already Implemented in MediaRepository

#### Get All Albums
```kotlin
suspend fun getAllAlbums(limit: Int = 50): Result<List<Album>>
```
- Uses: `getItems()` with `IncludeItemTypes=MusicAlbum`
- Fields: Name, AlbumArtist, ImageTags
- Returns: List of Album objects with cover URLs

#### Get All Artists
```kotlin
suspend fun getAllArtists(limit: Int = 100): Result<List<Artist>>
```
- Uses: `getItems()` with `IncludeItemTypes=MusicArtist`
- Fields: Name, PrimaryImageTag, ChildCount, SongCount
- Returns: List of Artist objects with image URLs and counts

#### Get All Songs
```kotlin
suspend fun getAllSongs(limit: Int = 100): Result<List<Song>>
```
- Uses: `getItems()` with `IncludeItemTypes=Audio`
- SortBy: `PlayCount,DatePlayed` (descending)
- Fields: Name, AlbumArtist, RunTimeTicks, ImageTags
- Returns: List of Song objects with duration and cover URLs

---

## UI/UX Enhancements

### Loading States
- Use Material 3 `CircularProgressIndicator`
- Center-aligned during initial load
- Pull-to-refresh for subsequent refreshes

### Error States
- Clear error messages
- Retry button
- Optional navigation back or to troubleshoot

### Empty States
- Friendly message: "No albums found" / "No artists found" / "No songs found"
- Suggestion to add media to Jellyfin server
- Optional refresh button

### Pull-to-Refresh
- Material 3 `PullToRefreshBox` (API 35+)
- Fallback: Manual refresh button in AppBar if API < 35

---

## Testing Strategy

### Manual Testing Checklist
- [ ] AllAlbumsScreen loads albums from API
- [ ] AllArtistsScreen loads artists from API
- [ ] AllSongsScreen loads songs from API
- [ ] Loading indicators display correctly
- [ ] Error messages display on network failure
- [ ] Empty states display when no data
- [ ] Pull-to-refresh works on all screens
- [ ] Back navigation works correctly
- [ ] Images load correctly (via Coil)
- [ ] Alphabet index works for artists
- [ ] Song durations display correctly
- [ ] Album counts and song counts display for artists

### Test Scenarios
1. **Happy Path**: Normal API response with data
2. **Empty Library**: API returns empty list
3. **Network Error**: Server unreachable
4. **Auth Error**: Invalid token
5. **Slow Network**: Loading states persist appropriately
6. **Large Dataset**: 100+ items load smoothly

---

## Future Enhancements (Out of Scope)

- [ ] Infinite scroll / pagination
- [ ] Search functionality within detail pages
- [ ] Sort options (alphabetical, date added, play count)
- [ ] Filter options (genre, year, etc.)
- [ ] Grid/List view toggle
- [ ] Caching with Room database
- [ ] Offline support
- [ ] Individual item detail pages (album details, artist details, song player)

---

## Implementation Order

1. ✅ Create specs document
2. ⏳ Create `AllAlbumsViewModel.kt`
3. ⏳ Update `AllAlbumsScreen.kt` to use ViewModel
4. ⏳ Create `AllArtistsViewModel.kt`
5. ⏳ Update `AllArtistsScreen.kt` to use ViewModel
6. ⏳ Create `AllSongsViewModel.kt`
7. ⏳ Update `AllSongsScreen.kt` to use ViewModel
8. ⏳ Test all three screens with real API
9. ⏳ Commit and push changes

---

## Success Criteria

- ✅ All three detail screens load real data from Jellyfin API
- ✅ Loading states implemented and functional
- ✅ Error handling implemented with user-friendly messages
- ✅ Pull-to-refresh functionality working
- ✅ No crashes or ANR issues
- ✅ UI remains responsive during data loading
- ✅ Images load correctly with proper placeholders
- ✅ Code follows existing architecture patterns (MVVM + Repository)

---

## Notes

- All ViewModels will use `viewModelScope` for coroutines
- MediaRepository already handles API initialization and token management
- No changes needed to `MediaRepository` or `JellyfinApiService`
- UI components (`AlbumCard`, `ArtistListItem`, `SongListItem`) remain unchanged
- Mock data files can remain for reference but won't be used
