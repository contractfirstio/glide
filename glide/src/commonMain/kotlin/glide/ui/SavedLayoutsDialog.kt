package glide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.AppSettingsStore
import glide.data.AppViewMode
import glide.data.WorkspacePreset
import glide.ui.layout.PanelWorkspace
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideTextButton

@Composable
fun SavedLayoutsDialog(
    viewMode: AppViewMode,
    onDismiss: () -> Unit,
) {
    val modeLabel = when (viewMode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> "Customer Management"
        AppViewMode.SCHEDULING -> "Scheduling"
    }
    val presets = PanelWorkspace.workspacePresets.filter { it.mode == viewMode.name }
    var presetToDelete by remember { mutableStateOf<WorkspacePreset?>(null) }

    if (presetToDelete != null) {
        val preset = presetToDelete!!
        DeleteConfirmDialog(
            title = "Delete saved layout?",
            message = "\"${preset.name}\" will be removed permanently.",
            onDismiss = { presetToDelete = null },
            onContinue = {
                PanelWorkspace.deletePreset(preset.id)
                AppSettingsStore.saveWorkspaceUi(PanelWorkspace.toSettings())
                presetToDelete = null
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved layouts") },
        text = {
            if (presets.isEmpty()) {
                Text(
                    text = "No saved layouts for $modeLabel yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        SavedLayoutRow(
                            preset = preset,
                            onApply = {
                                PanelWorkspace.applyPreset(preset)
                                AppSettingsStore.saveWorkspaceUi(PanelWorkspace.toSettings())
                                onDismiss()
                            },
                            onDelete = { presetToDelete = preset },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun SavedLayoutRow(
    preset: WorkspacePreset,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        GlideButton(onClick = onApply) {
            Text("Apply")
        }
        GlideTextButton(onClick = onDelete) {
            Text(
                text = "Delete",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
