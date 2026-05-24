package glide.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import glide.ui.theme.GlideButton

@Composable
fun BackupOfferDialog(
    title: String,
    message: String,
    onCreateBackup: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Not now")
            }
        },
        confirmButton = {
            GlideButton(onClick = onCreateBackup) {
                Text("Email backup")
            }
        },
    )
}
