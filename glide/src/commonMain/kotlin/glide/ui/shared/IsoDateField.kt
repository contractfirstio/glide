package glide.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.leads.millisToIsoDate
import glide.ui.leads.parseIsoDateToMillis
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideTextButton
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsoDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    yearRange: IntRange = (LocalDate.now().year - 2)..(LocalDate.now().year + 5),
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayValue = formatIsoDateForDisplay(value)
    val openPicker = { showPicker = true }

    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val pickerFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledContainerColor = Color.Transparent,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(modifier = modifier) {
        GlideFieldLabel(label)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = openPicker,
                    ),
            ) {
                OutlinedTextField(
                    value = displayValue,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    textStyle = textStyle,
                    placeholder = {
                        Text(
                            "Pick date",
                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = pickerFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GlideTextButton(onClick = openPicker) {
                Text("Pick")
            }
        }
        if (value.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                GlideTextButton(onClick = { onValueChange("") }) {
                    Text("Clear")
                }
            }
        }
    }

    if (showPicker) {
        val initialMillis = remember(value) {
            parseIsoDateToMillis(value)
                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = yearRange,
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millisToIsoDate(millis))
                        }
                        showPicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                GlideTextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = true,
                colors = DatePickerDefaults.colors(),
            )
        }
    }
}
