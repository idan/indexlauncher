package app.indexlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// White-on-dark controls remain legible over both light and dark wallpapers.
private val WallpaperColors = darkColorScheme(
    primary = Color(0xFFB1D9C5),
    onPrimary = Color(0xFF163D2D),
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color(0xFF14221B),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE0EAE1),
    outline = Color(0xFFD5E1D7),
)

// Material 3 Expressive favors a pronounced shape scale and clear typographic hierarchy.
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val ExpressiveTypography = Typography(
    headlineSmall = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun IndexLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WallpaperColors, shapes = ExpressiveShapes, typography = ExpressiveTypography) {
        CompositionLocalProvider(LocalContentColor provides WallpaperColors.onBackground, content = content)
    }
}

@Composable
fun IndexLauncherSettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = remember(context, dark) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colors, shapes = ExpressiveShapes, typography = ExpressiveTypography, content = content)
}
