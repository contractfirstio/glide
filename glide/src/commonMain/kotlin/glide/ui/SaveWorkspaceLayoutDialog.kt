package glide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.AppViewMode
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideOutlinedField

@Composable
fun SaveWorkspaceLayoutDialog(
    viewMode: AppViewMode,
    suggestedName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()
    val canSave = trimmed.isNotBlank()
    val modeLabel = when (viewMode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> "Customer Management"
        AppViewMode.SCHEDULING -> "Scheduling"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save workspace layout") },
        text = {
            Column {
                Text(
                    text = "Saves panel visibility and positions for $modeLabel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlideOutlinedField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Layout name",
                    placeholder = suggestedName,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            GlideButton(
                onClick = { onSave(trimmed) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
