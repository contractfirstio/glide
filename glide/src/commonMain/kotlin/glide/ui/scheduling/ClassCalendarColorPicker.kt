package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import glide.ui.theme.GlideTextButton

@Composable
fun ClassCalendarColorSwatch(
    colorArgb: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    size: Dp = 20.dp,
) {
    val color = Color(colorArgb)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                },
            )
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
    )
}

@Composable
fun ClassCalendarColorPickerDialog(
    currentColorArgb: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Calendar color", style = MaterialTheme.typography.titleSmall)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose a color for this class on the term calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClassCalendarPalette.forEach { argb ->
                        ClassCalendarColorSwatch(
                            colorArgb = argb,
                            selected = argb == currentColorArgb,
                            onClick = {
                                onColorSelected(argb)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

data class ClassColorPickerState(
    val show: (currentColorArgb: Int, onSelected: (Int) -> Unit) -> Unit,
    val dialog: @Composable () -> Unit,
)

@Composable
fun rememberClassColorPickerState(): ClassColorPickerState {
    var showPicker by remember { mutableStateOf(false) }
    var pickerColorArgb by remember { mutableStateOf(0) }
    var onSelected by remember { mutableStateOf<(Int) -> Unit>({}) }

    val dialog: @Composable () -> Unit = {
        if (showPicker) {
            ClassCalendarColorPickerDialog(
                currentColorArgb = pickerColorArgb,
                onColorSelected = { argb -> onSelected(argb) },
                onDismiss = { showPicker = false },
            )
        }
    }

    return ClassColorPickerState(
        show = { colorArgb, callback ->
            pickerColorArgb = colorArgb
            onSelected = callback
            showPicker = true
        },
        dialog = dialog,
    )
}
