package glide.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MidnightStudioColorScheme = darkColorScheme(
    primary = Color(0xFF9CC4E8),
    onPrimary = Color(0xFF0A141C),
    primaryContainer = Color(0xFF2A4558),
    onPrimaryContainer = Color(0xFFE8F4FC),
    secondary = Color(0xFFB8C4D0),
    onSecondary = Color(0xFF141820),
    secondaryContainer = Color(0xFF343D48),
    onSecondaryContainer = Color(0xFFE8ECF2),
    tertiary = Color(0xFF7EB0E0),
    onTertiary = Color(0xFF0A1420),
    tertiaryContainer = Color(0xFF243A52),
    onTertiaryContainer = Color(0xFFD8ECFA),
    background = Color(0xFF12161C),
    onBackground = Color(0xFFF0F4F8),
    surface = Color(0xFF1A2028),
    onSurface = Color(0xFFF0F4F8),
    surfaceVariant = Color(0xFF283038),
    onSurfaceVariant = Color(0xFFC4CDD6),
    outline = Color(0xFF5A6470),
    outlineVariant = Color(0xFF424C58),
    error = Color(0xFFFF9A8E),
    onError = Color(0xFF2D0A08),
)

private fun studioTypography(colors: ColorScheme): Typography {
    fun TextStyle.onSurface(): TextStyle = copy(color = colors.onSurface)
    fun TextStyle.onSurfaceVariant(): TextStyle = copy(color = colors.onSurfaceVariant)
    return Typography(
        headlineMedium = TextStyle(
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            color = colors.onSurface,
        ),
        titleMedium = TextStyle(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            color = colors.onSurface,
        ),
        titleSmall = TextStyle(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            color = colors.onSurface,
        ),
        bodyLarge = TextStyle(
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.05.sp,
            color = colors.onSurface,
        ),
        bodyMedium = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, color = colors.onSurface),
        bodySmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp, color = colors.onSurface),
        labelLarge = TextStyle(
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            color = colors.onSurface,
        ),
        labelMedium = TextStyle(
            fontSize = 9.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant,
        ),
        labelSmall = TextStyle(
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.1.sp,
            color = colors.onSurfaceVariant,
        ),
    )
}

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/** Primary list-row title color for selected vs unselected rows. */
@Composable
fun glideListItemTitleColor(selected: Boolean): Color =
    if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

@Composable
fun GlideTheme(content: @Composable () -> Unit) {
    val colors = MidnightStudioColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = studioTypography(colors),
        shapes = StudioShapes,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
            content()
        }
    }
}
