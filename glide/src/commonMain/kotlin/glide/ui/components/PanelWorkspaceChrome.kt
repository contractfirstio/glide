package glide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.AppViewState
import glide.ui.layout.GlideLayout
import glide.ui.layout.LayoutTier
import glide.ui.layout.PanelCatalog
import glide.ui.layout.PanelWorkspace
import glide.ui.theme.GlideAccents
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideTextButton

@Composable
fun CollapsedPanelDock(
    windowWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    val viewMode = AppViewState.mode
    val docked = PanelWorkspace.dockedPanelSlots
    if (docked.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Collapsed:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp),
        )
        docked.forEach { slot ->
            if (!PanelWorkspace.isVisible(slot, viewMode)) return@forEach
            val accent = GlideAccents.forPanel(slot)
            GlideOutlinedButton(
                onClick = {
                    PanelWorkspace.expandFromDock(slot, viewMode)
                    PanelWorkspace.focusPanel(slot, windowWidthPx, mode = viewMode)
                },
            ) {
                Text(
                    text = PanelCatalog.shortLabel(slot, viewMode),
                    color = accent,
                )
            }
        }
    }
}

@Composable
fun PanelTabBar(
    windowWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    val viewMode = AppViewState.mode
    if (GlideLayout.layoutTierFromPx(windowWidthPx) != LayoutTier.Tabbed) return

    val active = PanelWorkspace.tabActiveSlot
    val slots = PanelCatalog.primarySlots(viewMode).filter { PanelWorkspace.isVisible(it, viewMode) }
    if (slots.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEach { slot ->
            val selected = slot == active
            val accent = GlideAccents.forPanel(slot)
            if (selected) {
                GlideOutlinedButton(onClick = { PanelWorkspace.selectTab(slot, viewMode) }) {
                    Text(PanelCatalog.shortLabel(slot, viewMode), color = accent)
                }
            } else {
                GlideTextButton(onClick = { PanelWorkspace.selectTab(slot, viewMode) }) {
                    Text(
                        PanelCatalog.shortLabel(slot, viewMode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
