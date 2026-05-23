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

private val GlideDarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B4DC),
    onPrimary = Color(0xFF0A141C),
    primaryContainer = Color(0xFF1B3345),
    onPrimaryContainer = Color(0xFFC5E4F7),
    secondary = Color(0xFF9BA8B8),
    onSecondary = Color(0xFF141820),
    secondaryContainer = Color(0xFF2A313C),
    onSecondaryContainer = Color(0xFFD8DEE6),
    background = Color(0xFF0F1114),
    onBackground = Color(0xFFE4E6EA),
    surface = Color(0xFF16191D),
    onSurface = Color(0xFFE4E6EA),
    surfaceVariant = Color(0xFF23282E),
    onSurfaceVariant = Color(0xFFA8B0BA),
    outline = Color(0xFF454D57),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2D0A08),
)

private val DenseTypography = Typography(
    headlineMedium = TextStyle(fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
    bodyMedium = TextStyle(fontSize = 10.sp, lineHeight = 13.sp),
    bodySmall = TextStyle(fontSize = 10.sp, lineHeight = 12.sp),
    labelLarge = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 9.sp, lineHeight = 11.sp),
)

private val CompactShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
)

@Composable
fun GlideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlideDarkColorScheme,
        typography = DenseTypography,
        shapes = CompactShapes,
        content = content,
    )
}
