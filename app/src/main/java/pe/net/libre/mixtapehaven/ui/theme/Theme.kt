package pe.net.libre.mixtapehaven.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mixtape is a single fixed dark theme — no dynamic color, no light variant.
private val MixtapeColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = AccentInk,
    secondary = Coral,
    onSecondary = AccentInk,
    tertiary = Coral,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Stroke,
    outlineVariant = Stroke,
    error = Coral,
    onError = AccentInk,
    scrim = Color(0xCC000000),
)

@Composable
fun MixtapeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MixtapeColorScheme,
        typography = Typography,
        content = content,
    )
}
