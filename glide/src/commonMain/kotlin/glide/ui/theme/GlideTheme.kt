package glide.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MidnightStudioColorScheme = darkColorScheme(
    primary = Color(0xFF8CB4DC),
    onPrimary = Color(0xFF0A141C),
    primaryContainer = Color(0xFF1E3345),
    onPrimaryContainer = Color(0xFFD0E8F8),
    secondary = Color(0xFFA8B4C0),
    onSecondary = Color(0xFF141820),
    secondaryContainer = Color(0xFF2A313C),
    onSecondaryContainer = Color(0xFFD8DEE6),
    tertiary = Color(0xFF6E9FD4),
    onTertiary = Color(0xFF0A1420),
    tertiaryContainer = Color(0xFF1A2E42),
    onTertiaryContainer = Color(0xFFC8E0F4),
    background = Color(0xFF12161C),
    onBackground = Color(0xFFE4E8EC),
    surface = Color(0xFF1A2028),
    onSurface = Color(0xFFE4E8EC),
    surfaceVariant = Color(0xFF232A34),
    onSurfaceVariant = Color(0xFF9AA4B0),
    outline = Color(0xFF454D57),
    outlineVariant = Color(0xFF343C48),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2D0A08),
)

private val StudioTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.05.sp),
    bodyMedium = TextStyle(fontSize = 10.sp, lineHeight = 14.sp),
    bodySmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp),
    labelLarge = TextStyle(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp),
    labelMedium = TextStyle(fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.1.sp),
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun GlideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MidnightStudioColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content,
    )
}
