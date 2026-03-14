package pe.net.libre.mixtapehaven.ui.theme

import androidx.compose.ui.graphics.Color

// Retro-Modern Design Palette — aligned with Pencil design variables
// Grayscale base so album art colors can pop without competing with interface tints

// Base grayscale (--bg-page, --bg-card, --bg-elevated, --border-*, surface-active)
val BackgroundDeep  = Color(0xFF0C0C0C)   // --bg-page:      App background
val Surface         = Color(0xFF1A1A1A)   // --bg-card:      Cards, sheets
val SurfaceElevated = Color(0xFF242424)   // --bg-elevated:  Elevated cards, modals
val Border          = Color(0xFF2A2A2A)   // --border-default: Dividers, outlines
val BorderStrong    = Color(0xFF3A3A3A)   // --border-strong:  Stronger separators, strokes
val SurfaceActive   = Color(0xFF3A3A3A)   // Hover/active states (closest: --border-strong)

// Text (--text-primary, --text-secondary, --text-muted)
val TextPrimary     = Color(0xFFFFFFFF)   // --text-primary:   Primary text
val TextSecondary   = Color(0xFF8A8A8A)   // --text-secondary: Artist names, captions
val TextDisabled    = Color(0xFF525252)   // --text-muted:     Inactive elements

// Accent palette
val AccentPrimary   = Color(0xFF32D74B)   // --status-positive: Primary actions, CTA buttons
val AccentSecondary = Color(0xFFFFB74D)   // Warm amber — timestamps, secondary highlights
val AccentGlow      = Color(0xFF00FF00)   // Decorative only — use at 15–30% opacity
val AccentNeonCyan  = Color(0xFF00E5FF)   // Tertiary accent, links

// Semantic
val AccentRed       = Color(0xFFFF3B30)   // --accent-red:      Destructive actions
val AccentRedSoft   = Color(0x20FF3B30)   // --accent-red-soft: Destructive bg tint (~12% opacity)
val ErrorRed        = Color(0xFFFF453A)   // --status-negative: Error states
