# InstantMix Implementation Plan

## Overview

Add InstantMix support so users can generate a genre-based mix from any song, album, artist, or playlist. The Jellyfin server returns up to 200 shuffled tracks matching the source item's genres. The result is loaded into the playback queue.

---

## Step 1: API Layer

**File:** `data/api/JellyfinApiService.kt`

Add InstantMix endpoints to the Retrofit interface:

```kotlin
@GET("Songs/{itemId}/InstantMix")
suspend fun getSongInstantMix(
    @Path("itemId") itemId: String,
    @Query("UserId") userId: String,
    @Query("Limit") limit: Int? = 200,
    @Query("Fields") fields: String? = null
): ItemsResponse

@GET("Albums/{itemId}/InstantMix")
suspend fun getAlbumInstantMix(
    @Path("itemId") itemId: String,
    @Query("UserId") userId: String,
    @Query("Limit") limit: Int? = 200,
    @Query("Fields") fields: String? = null
): ItemsResponse

@GET("Artists/{itemId}/InstantMix")
suspend fun getArtistInstantMix(
    @Path("itemId") itemId: String,
    @Query("UserId") userId: String,
    @Query("Limit") limit: Int? = 200,
    @Query("Fields") fields: String? = null
): ItemsResponse

@GET("Playlists/{itemId}/InstantMix")
suspend fun getPlaylistInstantMix(
    @Path("itemId") itemId: String,
    @Query("UserId") userId: String,
    @Query("Limit") limit: Int? = 200,
    @Query("Fields") fields: String? = null
): ItemsResponse
```

The response type is `ItemsResponse` (already exists), which contains `List<BaseItemDto>` — same shape used everywhere else.

---

## Step 2: Repository Layer

**File:** `data/repository/MediaRepository.kt`

Add methods that call the new endpoints and map results to `List<Song>`:

```kotlin
suspend fun getSongInstantMix(songId: String, limit: Int = 200): Result<List<Song>>
suspend fun getAlbumInstantMix(albumId: String, limit: Int = 200): Result<List<Song>>
suspend fun getArtistInstantMix(artistId: String, limit: Int = 200): Result<List<Song>>
suspend fun getPlaylistInstantMix(playlistId: String, limit: Int = 200): Result<List<Song>>
```

Each uses the existing `apiCall` helper and `mapToSong()`. Fields parameter: `"PrimaryImageAspectRatio,Path,MediaSources"` (same as other song queries).

---

## Step 3: UI Entry Points

Add an "InstantMix" action in contexts where it makes sense. This is a button/menu item that fetches the mix and loads it into the queue.

### 3a. Song context — NowPlayingScreen

**File:** `ui/nowplaying/NowPlayingScreen.kt`

Add an InstantMix icon button (e.g. `Icons.AutoMirrored.Filled.Sort` or a radio icon) to the NowPlaying toolbar/controls. When tapped, it calls the ViewModel to start an instant mix from the currently playing song.

**File:** `ui/nowplaying/NowPlayingViewModel.kt`

Add:
```kotlin
fun startInstantMix() {
    val currentSong = playbackManager.playbackState.value.currentSong ?: return
    viewModelScope.launch {
        _uiState.update { it.copy(isLoadingMix = true) }
        mediaRepository.getSongInstantMix(currentSong.id)
            .onSuccess { songs ->
                if (songs.isNotEmpty()) {
                    playbackManager.setQueue(songs, 0)
                }
            }
            .onFailure { /* show error */ }
        _uiState.update { it.copy(isLoadingMix = false) }
    }
}
```

Note: `NowPlayingViewModel` currently doesn't take `mediaRepository` — it will need to accept it as a constructor parameter, and the NavGraph must pass it through.

### 3b. Artist context — ArtistDetailScreen

**File:** `ui/artist/ArtistDetailScreen.kt` / `ArtistDetailViewModel.kt`

Add an "InstantMix" button alongside the existing "Play All" and "Shuffle" buttons. Calls `mediaRepository.getArtistInstantMix(artistId)` and loads into queue.

### 3c. Playlist context — PlaylistDetailScreen

**File:** `ui/playlist/PlaylistDetailScreen.kt` / `PlaylistDetailViewModel.kt`

Add an "InstantMix" button alongside existing controls. Calls `mediaRepository.getPlaylistInstantMix(playlistId)` and loads into queue.

### 3d. Song long-press / overflow menu (optional, future)

Could add InstantMix to the song overflow menu (`MoreVert` icon) in `SongListItem`. This is lower priority and can be a follow-up.

---

## Step 4: Wire Up in NavGraph

**File:** `ui/navigation/NavGraph.kt`

Pass `mediaRepository` to `NowPlayingScreen` (currently only receives `playbackManager`). The `NowPlayingViewModel` factory needs updating to accept `mediaRepository`.

---

## Summary of Files Changed

| File | Change |
|------|--------|
| `data/api/JellyfinApiService.kt` | Add 4 InstantMix endpoint methods |
| `data/repository/MediaRepository.kt` | Add 4 InstantMix repository methods |
| `ui/nowplaying/NowPlayingViewModel.kt` | Add `startInstantMix()`, accept `mediaRepository` |
| `ui/nowplaying/NowPlayingScreen.kt` | Add InstantMix button, pass `mediaRepository` to VM |
| `ui/artist/ArtistDetailViewModel.kt` | Add `startInstantMix()` method |
| `ui/artist/ArtistDetailScreen.kt` | Add InstantMix button |
| `ui/playlist/PlaylistDetailViewModel.kt` | Add `startInstantMix()` method |
| `ui/playlist/PlaylistDetailScreen.kt` | Add InstantMix button |
| `ui/navigation/NavGraph.kt` | Pass `mediaRepository` to NowPlayingScreen |

No new files needed. No new dependencies. The existing `ItemsResponse` and `mapToSong()` handle everything.
