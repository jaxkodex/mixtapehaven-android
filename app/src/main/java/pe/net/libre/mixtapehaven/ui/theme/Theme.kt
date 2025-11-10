package pe.net.libre.mixtapehaven.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Seamless Hyper-Efficiency Color Scheme
 * AMOLED Black theme for battery efficiency and futuristic aesthetic
 */
private val MixtapeHavenColorScheme = darkColorScheme(
    // Primary colors - Cyber Neon Blue for CTAs
    primary = CyberNeonBlue,
    onPrimary = DeepSpaceBlack,
    primaryContainer = CyberNeonBlue,
    onPrimaryContainer = DeepSpaceBlack,

    // Secondary colors - Vaporwave Magenta for highlights
    secondary = VaporwaveMagenta,
    onSecondary = DeepSpaceBlack,
    secondaryContainer = VaporwaveMagenta,
    onSecondaryContainer = DeepSpaceBlack,

    // Tertiary colors - Additional accent
    tertiary = CyberNeonBlue,
    onTertiary = DeepSpaceBlack,
    tertiaryContainer = GunmetalGray,
    onTertiaryContainer = LunarWhite,

    // Background - AMOLED Black
    background = DeepSpaceBlack,
    onBackground = LunarWhite,

    // Surface - Dark surfaces with Gunmetal Gray
    surface = DeepSpaceBlack,
    onSurface = LunarWhite,
    surfaceVariant = GunmetalGray,
    onSurfaceVariant = LunarWhite,

    // Surface tints
    surfaceTint = CyberNeonBlue,

    // Inverse colors
    inverseSurface = LunarWhite,
    inverseOnSurface = DeepSpaceBlack,
    inversePrimary = CyberNeonBlue,

    // Error colors
    error = VaporwaveMagenta,
    onError = DeepSpaceBlack,
    errorContainer = VaporwaveMagenta,
    onErrorContainer = DeepSpaceBlack,

    // Outline colors
    outline = GunmetalGray,
    outlineVariant = GunmetalGray,

    // Scrim
    scrim = DeepSpaceBlack,
)

@Composable
fun MixtapeHavenTheme(
    darkTheme: Boolean = true, // Always use dark theme for AMOLED
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