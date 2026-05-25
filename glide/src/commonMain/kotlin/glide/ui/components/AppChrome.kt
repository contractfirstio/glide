package glide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import glide.data.AppSettingsStore
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.generated.resources.Res
import glide.generated.resources.glide_logo
import glide.ui.SaveWorkspaceLayoutDialog
import glide.ui.SavedLayoutsDialog
import glide.ui.layout.GlideLayout
import glide.ui.layout.PanelCatalog
import glide.ui.layout.PanelWorkspace
import glide.ui.theme.GlideOutlinedButton
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppChrome(
    windowWidthPx: Int,
    modifier: Modifier = Modifier,
    companySettingsNeedSetup: Boolean = false,
    companySettingsConfigured: Boolean = true,
    onOpenCompanySettings: () -> Unit = {},
    onEmailDataBackup: () -> Unit = {},
) {
    val mode = AppViewState.mode
    val compactChrome = with(androidx.compose.ui.platform.LocalDensity.current) {
        windowWidthPx.toDp() < GlideLayout.CompactChromeWidthBreakpoint
    }
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
        Image(
            painter = painterResource(Res.drawable.glide_logo),
            contentDescription = "Glide",
            modifier = Modifier.height(28.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewModeButton(
                label = if (compactChrome) "Customers" else "Customer Management",
                selected = mode == AppViewMode.CUSTOMER_MANAGEMENT,
                onClick = { AppViewState.switchTo(AppViewMode.CUSTOMER_MANAGEMENT) },
            )
            ViewModeButton(
                label = if (compactChrome) "Schedule" else "Scheduling",
                selected = mode == AppViewMode.SCHEDULING,
                onClick = { AppViewState.switchTo(AppViewMode.SCHEDULING) },
            )
            AppMenu(
                windowWidthPx = windowWidthPx,
                companySettingsNeedSetup = companySettingsNeedSetup,
                companySettingsConfigured = companySettingsConfigured,
                onOpenCompanySettings = onOpenCompanySettings,
                onEmailDataBackup = onEmailDataBackup,
            )
        }
    }
}

@Composable
private fun AppMenu(
    windowWidthPx: Int,
    companySettingsNeedSetup: Boolean,
    companySettingsConfigured: Boolean,
    onOpenCompanySettings: () -> Unit,
    onEmailDataBackup: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var panelsExpanded by remember { mutableStateOf(false) }
    var showSaveLayoutDialog by remember { mutableStateOf(false) }
    var showSavedLayoutsDialog by remember { mutableStateOf(false) }
    val viewMode = AppViewState.mode
    val presets = PanelWorkspace.workspacePresets.filter { it.mode == viewMode.name }

    if (showSaveLayoutDialog) {
        SaveWorkspaceLayoutDialog(
            viewMode = viewMode,
            suggestedName = "Layout ${presets.size + 1}",
            onSave = { name ->
                PanelWorkspace.savePreset(name, viewMode)
                AppSettingsStore.saveWorkspaceUi(PanelWorkspace.toSettings())
                showSaveLayoutDialog = false
            },
            onDismiss = { showSaveLayoutDialog = false },
        )
    }

    if (showSavedLayoutsDialog) {
        SavedLayoutsDialog(
            viewMode = viewMode,
            onDismiss = { showSavedLayoutsDialog = false },
        )
    }

    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (companySettingsNeedSetup) {
                TextButton(onClick = onOpenCompanySettings) {
                    Text(
                        text = "Set up company",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = { menuExpanded = true }) {
                Text(
                    text = "⋯",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
                panelsExpanded = false
            },
        ) {
            DropdownMenuItem(
                text = { Text("Panels ▸") },
                onClick = { panelsExpanded = !panelsExpanded },
            )
            if (panelsExpanded) {
                PanelCatalog.primarySlots(viewMode).forEach { slot ->
                    val visible = PanelWorkspace.isVisible(slot, viewMode)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = visible, onCheckedChange = null)
                                Text(PanelCatalog.label(slot, viewMode))
                            }
                        },
                        onClick = {
                            PanelWorkspace.setVisible(slot, !visible, viewMode)
                            AppSettingsStore.saveWorkspaceUi(PanelWorkspace.toSettings())
                        },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Save workspace layout…") },
                onClick = {
                    menuExpanded = false
                    panelsExpanded = false
                    showSaveLayoutDialog = true
                },
            )
            DropdownMenuItem(
                text = { Text("Saved layouts…") },
                onClick = {
                    menuExpanded = false
                    panelsExpanded = false
                    showSavedLayoutsDialog = true
                },
            )
            DropdownMenuItem(
                text = { Text("Restore default layout") },
                onClick = {
                    PanelWorkspace.restoreDefaultLayout(viewMode)
                    AppSettingsStore.saveWorkspaceUi(PanelWorkspace.toSettings())
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Company settings…") },
                onClick = {
                    menuExpanded = false
                    onOpenCompanySettings()
                },
            )
            DropdownMenuItem(
                text = { Text("Email data backup…") },
                enabled = companySettingsConfigured,
                onClick = {
                    menuExpanded = false
                    onEmailDataBackup()
                },
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
        TextButton(onClick = onClick) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
