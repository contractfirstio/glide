package glide.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideTextButton

@Composable
fun DeleteActionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    blockedReason: String?,
    modifier: Modifier = Modifier,
) {
    var showBlockedDialog by remember { mutableStateOf(false) }
    val isBlocked = !enabled || !blockedReason.isNullOrBlank()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GlideOutlinedButton(
            onClick = {
                if (isBlocked) {
                    showBlockedDialog = true
                } else {
                    onClick()
                }
            },
            enabled = true,
        ) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Cannot delete") },
            text = {
                Text(
                    blockedReason
                        ?: "This item cannot be deleted right now. Resolve linked records first.",
                )
            },
            confirmButton = {
                GlideTextButton(onClick = { showBlockedDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}
