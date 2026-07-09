package dev.alifhasnain.falconer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A quiet, composed light theme (v1 is light-only; dark is future work).
 *
 * Neutral surfaces keep chrome receded so the payload is the loudest thing on
 * screen (PRODUCT principle 1). Status/method colors carry meaning, not
 * decoration. Body text is monospaced and high-contrast (>= 4.5:1).
 */
object FalconerColors {
    val Background = Color(0xFFF5F6F8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEEF0F3)
    val Ink = Color(0xFF16191D)
    val InkSecondary = Color(0xFF565E6B) // ~5:1 on Surface
    val Outline = Color(0xFFD9DDE3)
    val Accent = Color(0xFF3457D5)

    // HTTP status / method semantics.
    val Green = Color(0xFF1B7F3B)
    val Blue = Color(0xFF1565C0)
    val Amber = Color(0xFFA65B00)
    val Red = Color(0xFFC02626)
    val Neutral = Color(0xFF6B7280)
}

/** Color for an HTTP status code. Null (in-flight / no response) is neutral. */
fun statusColor(code: Int?): Color = when {
    code == null -> FalconerColors.Neutral
    code in 200..299 -> FalconerColors.Green
    code in 300..399 -> FalconerColors.Blue
    code in 400..499 -> FalconerColors.Amber
    code >= 500 -> FalconerColors.Red
    else -> FalconerColors.Neutral
}

/** Color for an HTTP method label. */
fun methodColor(method: String): Color = when (method.uppercase()) {
    "GET" -> FalconerColors.Blue
    "POST" -> FalconerColors.Green
    "PUT", "PATCH" -> FalconerColors.Amber
    "DELETE" -> FalconerColors.Red
    else -> FalconerColors.Neutral
}

private val FalconerColorScheme = lightColorScheme(
    primary = FalconerColors.Accent,
    onPrimary = Color.White,
    background = FalconerColors.Background,
    onBackground = FalconerColors.Ink,
    surface = FalconerColors.Surface,
    onSurface = FalconerColors.Ink,
    surfaceVariant = FalconerColors.SurfaceMuted,
    onSurfaceVariant = FalconerColors.InkSecondary,
    outline = FalconerColors.Outline,
    error = FalconerColors.Red,
    // Menus, dropdowns and dialogs pull their container from the surfaceContainer*
    // roles. Left unset they fall back to M3's baseline purple palette (the
    // "reddish" tint); map them to Falconer neutrals so popups match the app.
    surfaceContainerLowest = FalconerColors.Surface,
    surfaceContainerLow = FalconerColors.Surface,
    surfaceContainer = FalconerColors.Surface,
    surfaceContainerHigh = FalconerColors.SurfaceMuted,
    surfaceContainerHighest = FalconerColors.SurfaceMuted,
    // Flat, composed look — no tonal elevation overlay recoloring surfaces.
    surfaceTint = Color.Transparent,
)

/** Monospace style used for URLs, headers and bodies — the payload type. */
val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 19.sp,
)

private val FalconerTypography = Typography(
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun FalconerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FalconerColorScheme,
        typography = FalconerTypography,
        content = content,
    )
}
