package pe.net.libre.mixtapehaven.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Retro-Modern Color Scheme
 * Grayscale base with green/amber accents so album art can pop without competing tints
 */
private val MixtapeHavenColorScheme = darkColorScheme(
    // Primary - Accent green for CTAs and active states
    primary = AccentPrimary,
    onPrimary = BackgroundDeep,
    primaryContainer = SurfaceActive,
    onPrimaryContainer = TextPrimary,

    // Secondary - Warm amber for timestamps and secondary highlights
    secondary = AccentSecondary,
    onSecondary = BackgroundDeep,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,

    // Tertiary - Neon cyan for links and tertiary accents
    tertiary = AccentNeonCyan,
    onTertiary = BackgroundDeep,
    tertiaryContainer = SurfaceElevated,
    onTertiaryContainer = TextPrimary,

    // Background
    background = BackgroundDeep,
    onBackground = TextPrimary,

    // Surface
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,

    // Surface tints
    surfaceTint = AccentPrimary,

    // Inverse colors
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDeep,
    inversePrimary = AccentPrimary,

    // Error colors
    error = ErrorRed,
    onError = BackgroundDeep,
    errorContainer = SurfaceElevated,
    onErrorContainer = ErrorRed,

    // Outline colors
    outline = Border,
    outlineVariant = SurfaceElevated,

    // Scrim
    scrim = BackgroundDeep,
)

@Composable
fun MixtapeHavenTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = MixtapeHavenColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}