package glide.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import glide.ui.theme.GlideButton

@Composable
fun BackupResultDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data backup") },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            GlideButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
