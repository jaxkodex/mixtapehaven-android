# Floating Now Playing Component Implementation

## Overview
This document outlines the implementation of the floating "now playing" component and full-screen "Now Playing" view for the Mixtape Haven Android app.

## Reference Designs
- **Floating Component**: `specs/screens/screen_floating_playing_now.png`
- **Full Screen**: `specs/screens/screen_now_playing.png`

## Components to Implement

### 1. Enhanced Floating Now Playing Bar (NowPlayingBar.kt)
**Location**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/home/components/NowPlayingBar.kt`

**Features**:
- Album artwork thumbnail (rounded corners)
- Song title and artist name
- Horizontal progress bar (showing playback progress)
- Play/Pause button
- Click to expand to full screen
- Elevation/shadow for floating effect

**Updates Required**:
- Add progress bar parameter (current position, total duration)
- Update styling to match design specs
- Ensure proper state management for play/pause toggle

### 2. Full Now Playing Screen (NowPlayingScreen.kt)
**Location**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/nowplaying/NowPlayingScreen.kt`

**Features**:
- Full-screen black background
- Large album artwork (centered, rounded corners)
- Song title (large, bold)
- Artist name (smaller, lighter)
- Progress bar with timestamps (current time / total duration)
- Playback controls:
  - Previous track button
  - Play/Pause button (large, centered, cyan)
  - Next track button
- Bottom action buttons:
  - Lyrics
  - Equalizer
  - Add to playlist
- Back button (top-left)
- More options button (top-right)

**Layout**:
```
┌─────────────────────┐
│ ← NOW PLAYING    ⋮  │
│                     │
│   ┌───────────┐     │
│   │           │     │
│   │  Album    │     │
│   │   Art     │     │
│   │           │     │
│   └───────────┘     │
│                     │
│    Song Title       │
│    Artist Name      │
│                     │
│ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ │
│ 1:17         4:15   │
│                     │
│   ⏮  ⏯  ⏭          │
│                     │
│                     │
│  🎵  📊  ➕         │
│ Lyrics Eq  Playlist│
└─────────────────────┘
```

### 3. Playback State Management
**Location**: `app/src/main/java/pe/net/libre/mixtapehaven/data/playback/PlaybackManager.kt`

**Features**:
- Singleton/shared playback state
- Current song information
- Play/Pause state
- Progress tracking (current position, duration)
- Play, pause, next, previous controls
- Observable state flow for UI updates

**Data Model**:
```kotlin
data class PlaybackState(
    val currentSong: Song?,
    val isPlaying: Boolean,
    val currentPosition: Long, // milliseconds
    val duration: Long, // milliseconds
    val progress: Float // 0.0 to 1.0
)
```

### 4. Now Playing ViewModel
**Location**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/nowplaying/NowPlayingViewModel.kt`

**Responsibilities**:
- Observe PlaybackManager state
- Handle user interactions (play, pause, seek, next, previous)
- Manage lyrics, equalizer, playlist actions
- Format time strings (e.g., "1:17", "4:15")

### 5. Navigation Updates
**Location**: `app/src/main/java/pe/net/libre/mixtapehaven/ui/navigation/NavGraph.kt`

**Changes**:
- Add `NowPlaying` route to sealed class `Screen`
- Add composable destination for NowPlayingScreen
- Pass PlaybackManager to NowPlayingScreen
- Update HomeViewModel to navigate to NowPlaying screen

## Implementation Steps

1. **Create PlaybackManager** (data/playback/PlaybackManager.kt)
   - Implement state management
   - Add methods for play/pause/next/previous
   - Add progress tracking

2. **Update Song Data Model** (ui/home/MockData.kt)
   - Ensure Song has all necessary fields
   - Add album name if needed

3. **Enhance NowPlayingBar** (ui/home/components/NowPlayingBar.kt)
   - Add progress bar
   - Update styling to match design
   - Add proper play/pause state

4. **Create NowPlayingScreen** (ui/nowplaying/NowPlayingScreen.kt)
   - Implement full-screen layout
   - Add all controls and buttons
   - Style according to design specs

5. **Create NowPlayingViewModel** (ui/nowplaying/NowPlayingViewModel.kt)
   - Connect to PlaybackManager
   - Handle all user interactions
   - Format time strings

6. **Update Navigation** (ui/navigation/NavGraph.kt)
   - Add NowPlaying route
   - Wire up navigation

7. **Update HomeViewModel** (ui/home/HomeViewModel.kt)
   - Connect to PlaybackManager
   - Implement onSongClick to start playback
   - Implement onNowPlayingBarClick navigation

8. **Update MainActivity** (MainActivity.kt)
   - Initialize PlaybackManager
   - Pass to navigation

## Design Specifications

### Colors (from existing theme)
- Background: `DeepSpaceBlack` (black)
- Primary accent: `CyberNeonBlue` (cyan)
- Secondary accent: `VaporwaveMagenta` (magenta/pink)
- Text: `LunarWhite` (white)
- Surface: `GunmetalGray` (dark gray)

### Typography
- Song title: `headlineMedium` (large, bold)
- Artist name: `bodyLarge` (medium, 70% opacity)
- Timestamps: `bodySmall` (small)

### Spacing
- Component padding: 16dp
- Element spacing: 8dp, 12dp, 16dp
- Album art size: 48dp (floating), ~300dp (full screen)
- Button sizes: 56dp (large), 40dp (medium), 32dp (small)

### Animations (Future Enhancement)
- Smooth expand/collapse transition from floating to full screen
- Progress bar animation
- Button press feedback

## Testing Checklist
- [ ] Floating bar appears when song is clicked
- [ ] Progress bar updates in real-time
- [ ] Play/Pause button toggles correctly
- [ ] Clicking floating bar navigates to full screen
- [ ] Full screen displays all song information
- [ ] All playback controls work (play, pause, next, previous)
- [ ] Back button returns to previous screen
- [ ] Progress bar is seekable (if implemented)
- [ ] Timestamps update correctly
- [ ] Layout looks correct on different screen sizes

## Future Enhancements
- Audio playback integration with Jellyfin
- Seek functionality on progress bar
- Lyrics display
- Equalizer integration
- Add to playlist functionality
- Queue management
- Shuffle and repeat modes
- Background playback with notification controls
