package pe.net.libre.mixtapehaven.ui.theme

import androidx.compose.ui.graphics.Color

// Retro-Modern Design Palette
// Grayscale base so album art colors can pop without competing with interface tints

// Base grayscale
val BackgroundDeep  = Color(0xFF0D0D0D)   // App background
val Surface         = Color(0xFF1A1A1A)   // Cards, sheets
val SurfaceElevated = Color(0xFF242424)   // Elevated cards, modals
val Border          = Color(0xFF2E2E2E)   // Dividers, outlines
val SurfaceActive   = Color(0xFF3E3E42)   // Hover/active states
val TextPrimary     = Color(0xFFF5F5F5)   // Primary text (off-white)
val TextSecondary   = Color(0xFFA0A0A0)   // Artist names, captions
val TextDisabled    = Color(0xFF666666)   // Inactive elements

// Accent palette
val AccentPrimary   = Color(0xFF1ED760)   // Primary actions, progress bar, CTA buttons
val AccentSecondary = Color(0xFFFFB74D)   // Timestamps, secondary highlights
val AccentGlow      = Color(0xFF00FF00)   // Decorative only — use at 15–30% opacity
val AccentNeonCyan  = Color(0xFF00E5FF)   // Tertiary accent, links

// Semantic
val ErrorRed        = Color(0xFFCF6679)   // Error states (WCAG-compliant on dark)