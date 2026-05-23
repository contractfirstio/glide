package glide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.ui.layout.GlideLayout
import glide.ui.theme.GlideOutlinedButton

@Composable
fun AppChrome(modifier: Modifier = Modifier) {
    val mode = AppViewState.mode
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GlideLayout.AppChromeHeight)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A222C),
                        Color(0xFF151A22),
                    ),
                ),
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Glide",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewModeButton(
                label = "Customer Management",
                selected = mode == AppViewMode.CUSTOMER_MANAGEMENT,
                onClick = { AppViewState.switchTo(AppViewMode.CUSTOMER_MANAGEMENT) },
            )
            ViewModeButton(
                label = "Scheduling",
                selected = mode == AppViewMode.SCHEDULING,
                onClick = { AppViewState.switchTo(AppViewMode.SCHEDULING) },
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        GlideOutlinedButton(onClick = onClick) {
            Text(label)
        }
    } else {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
