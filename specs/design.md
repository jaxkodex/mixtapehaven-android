## Design language specification

### Color system

The app should be **dark-first**, using a neutral grayscale base so album art colors can pop without competing with interface tints. Analysis of 50+ dark-mode apps confirms that major music players use pure grayscale (no blue or warm tinting) for this exact reason.

**Base palette:**

| Token | Hex | Usage |
|-------|-----|-------|
| `background-deep` | `#0D0D0D` | App background |
| `surface` | `#1A1A1A` | Cards, sheets |
| `surface-elevated` | `#242424` | Elevated cards, modals |
| `border` | `#2E2E2E` | Dividers, outlines |
| `surface-active` | `#3E3E42` | Hover/active states |
| `text-primary` | `#F5F5F5` | Primary text (off-white) |
| `text-secondary` | `#A0A0A0` | Artist names, captions |
| `text-disabled` | `#666666` | Inactive elements |

**Retro accent palette:** The classic Winamp green (`#00FF00`) fails WCAG at only **1.4:1 contrast** on dark backgrounds — it must be modified for any text or interactive use. A recommended accessible variant is `#1ED760` (close to Spotify's green, passes at 3:1+ on `#0D0D0D` for large text). For decorative-only elements (spectrum bars, visualizer glows), pure `#00FF00` can be used at reduced opacity.

| Token | Hex | Usage |
|-------|-----|-------|
| `accent-primary` | `#1ED760` | Primary actions, active progress bar, CTA buttons |
| `accent-secondary` | `#FFB74D` | Warm amber — timestamps, secondary highlights |
| `accent-glow` | `#00FF00` at 15–30% opacity | Decorative spectrum bars, visualizer elements |
| `accent-neon-cyan` | `#00E5FF` | Tertiary accent, link color |

**Dynamic color from album art** should be implemented using the Android Palette API or Vibrant.js, with mandatory luminance checks gating every background application. Fallback to `#0D0D0D` when extraction fails. Target minimum **4.5:1** contrast for text overlay on dynamically-colored backgrounds.

### Typography system

Two font families maximum. The primary font provides the clean modern foundation; the monospace accent introduces retro character at specific touchpoints.

**Primary:** **Inter** — open-source, variable weight, designed for screens, large x-height, excellent small-size legibility. Cross-platform consistent (unlike SF Pro or Roboto which are platform-locked). Alternatively, use **Space Grotesk** for slightly more geometric personality.

**Accent:** **JetBrains Mono** — modern monospace with ligature support and strong readability. Applied to: elapsed/remaining timestamps, bitrate and audio quality labels (`320kbps`, `FLAC`), equalizer frequency values (`60Hz`, `4kHz`), and file size/format metadata. This creates the retro-technical feel of Winamp's LED displays without compromising the overall modern tone.

**Type scale (mobile, 8pt grid):**

| Role | Size | Line height | Weight | Font |
|------|------|-------------|--------|------|
| Now Playing title | 32px | 40px | Bold 700 | Inter |
| Screen titles | 24px | 32px | Semibold 600 | Inter |
| Section headers | 20px | 28px | Semibold 600 | Inter |
| Song names in lists | 17px | 24px | Medium 500 | Inter |
| Body/descriptions | 15px | 24px | Regular 400 | Inter |
| Artist/album captions | 13px | 20px | Regular 400 | Inter |
| Timestamps, tech labels | 13px | 20px | Regular 400 | JetBrains Mono |
| Overlines, bitrate badges | 11px | 16px | Medium 500 | JetBrains Mono |

All line heights align to 4pt increments. Spacing uses 8pt increments (8, 16, 24, 32, 40, 48px) with 4pt half-steps for fine adjustments like icon-to-text gaps.

### Motion and animation principles

Motion should feel physically grounded — spring physics over cubic-bezier easing — with retro-flavored ambient animations as optional overlays.

**Core transitions.** The mini-player to full now-playing expansion is the app's signature transition. It should use a **hero animation** (shared element transition) where the album art thumbnail continuously morphs from the mini-player into the full-screen artwork, driven by spring physics with damping ratio ~0.8 and a duration of **400–500ms**. This must be gesture-interactive — following the user's finger, cancellable mid-drag — not a canned animation. On Android, implement with `MotionLayout` and `OnSwipe`; on iOS, with `UIPanGestureRecognizer` and `UIViewPropertyAnimator` using `UISpringTimingParameters`.

**Playback state animations.** The play/pause icon should smoothly morph between states using SVG path interpolation (via Lottie or custom animation) at **200–300ms** with Material standard easing. The "like" heart button benefits from a spring scale overshoot (damping 0.6–0.8, ~400ms). Progress bars update at 60fps with an optional subtle glow at the playhead position in accent green.

**Nostalgic ambient animations.** A simplified spectrum analyzer — thin bars with retro green or amber coloring — can appear on the now-playing screen, driven by low-frequency audio amplitude data. Audio-reactive glow around album art (5–15% opacity pulsing synced to bass frequencies) adds atmosphere without distraction. These must be optional with a "reduce motion" toggle for accessibility. A Winamp-heritage MilkDrop-style procedural visualizer could serve as an opt-in full-screen mode for engaged listeners.

| Animation type | Duration | Easing |
|---------------|----------|--------|
| Button press feedback | 100–150ms | ease-out |
| Icon morph (play↔pause) | 200–300ms | Material standard |
| Screen transitions | 300–350ms | spring, damping 0.85 |
| Mini → full player | 400–500ms | spring, damping 0.8 |
| Dynamic color background shift | 600–800ms | ease-in-out |
| Staggered list item appearance | 50ms between items | ease-out |

### Component specifications

**Player controls (transport).** Horizontally centered: skip-back, play/pause (largest at **56–64px** touch target, circular container), skip-forward. Shuffle and repeat as smaller secondary controls (40px) flanking the transport row. All targets meet the 44pt minimum. The play/pause button is the visual anchor — consider a subtle neumorphic press effect on this single element as a tactile nod to physical buttons, while keeping the rest flat.

**Progress bar / scrubber.** Default to a minimal thin line (2–4px height) with a playhead dot that expands from 0 to 12–16px on touch. Elapsed and remaining time displayed in JetBrains Mono at 13px (the retro accent touchpoint). As an opt-in premium/power-user feature, offer a **waveform visualization** showing amplitude data — styled with `#1ED760` for played portions and `#3E3E42` for unplayed. Waveform requires pre-processing (100 data points per track is sufficient) and is not feasible for massive streaming catalogs without server-side computation.

**Album art treatment.** Centered on the now-playing screen with 24–32px horizontal padding. Rounded corners at **8–12px** radius. On dark backgrounds, drop shadows are less visible, so use a **dominant-color ambient glow** instead: `box-shadow: 0 8px 32px rgba(dominant_color, 0.3)`. This creates depth while reinforcing the dynamic color system.

**Mini-player.** Fixed above the tab bar, 56–64px height. Shows: album art thumbnail (40–48px square), song title (truncated), artist name (secondary text), play/pause button. A thin 2px accent-colored progress bar runs along the top edge. Swipe gestures: up to expand, left/right to skip (with haptic feedback), tap to expand. The progress bar and timestamp elements use the monospace accent font for retro character.

**Equalizer.** Dedicated screen with preset buttons (Flat, Bass Boost, Treble Boost, Vocal, Rock, Custom) and draggable band sliders. This is the strongest candidate for concentrated retro styling — monospace frequency labels (`60Hz`, `250Hz`, `1kHz`, `4kHz`, `16kHz`), neon-accented slider tracks, and optionally an LED-style gain readout. The equalizer is where users who love Winamp's aesthetic will find the deepest resonance, and its contained scope means heavier retro styling won't affect the broader app's usability.

### Information architecture

**Three primary tabs plus a floating now-playing layer** is the optimal structure. Research shows tab bars are far more discoverable than hamburger menus — Spotify abandoned its hamburger menu in 2016 for exactly this reason.

| Tab | Content | Rationale |
|-----|---------|-----------|
| **Home** | Recently played, recommendations, daily mixes, new releases | Entry point for both returners and discoverers |
| **Search** | Text search, browse by genre/mood/era | Consolidates all discovery into one hub |
| **Library** | Playlists, Artists, Albums, Songs, Downloads (with filter tabs) | The "owned music" experience — core to the retro-modern philosophy |

**Now Playing** is not a tab — it's a persistent mini-player bar above the tab bar that expands to a full-screen modal via gesture. **Settings** lives behind a profile icon in the Home screen's top navigation bar, keeping the tab bar to three clean items.

**Queue management** should be accessible from the now-playing screen via a dedicated icon, presented as a **half-screen bottom sheet** that's draggable to full-screen. It must support drag-to-reorder, swipe-to-remove, clear queue, and "Save queue as playlist." Provide both "Play Next" (insert after current track) and "Add to Queue" (append to end) as distinct, clearly labeled actions throughout the app — this alone addresses the most common usability complaint across both Spotify and Apple Music.

**Audio settings** deserve a dedicated subsection: Playback (crossfade slider 0–12s, gapless toggle, automix), Audio Quality (streaming and download quality with bitrate labels in monospace: `96kbps` / `160kbps` / `320kbps` / `FLAC`), and Equalizer. The retro monospace treatment of technical audio values throughout settings reinforces the brand identity at every touchpoint.

### Cross-platform strategy

The retro-modern design language should be implemented as a **brand layer above platform conventions**, following Poolsuite's proven model. Navigation (tab bar structure, back navigation, gesture handling), system integration (lock screen controls, AirPlay/Cast, CarPlay/Android Auto), and accessibility features use native platform patterns without modification. The brand layer — custom colors, Inter + JetBrains Mono typography, retro accent animations, album art treatment, and visualizer features — is shared cross-platform as a unified design token system.

On Android, Material You's dynamic wallpaper-based theming may conflict with the app's retro palette. Offer a "Retro Mode" theme that overrides system dynamic color with the curated accent palette, while providing a "System" theme option for users who prefer Material You integration. On iOS, leverage system colors for interactive elements (tint colors, selection states) while applying custom retro colors to decorative content elements. Lock screen controls, notification media widgets, and CarPlay/Android Auto interfaces must strictly follow platform guidelines — retro theming in these contexts is limited to album art styling and font choices only.