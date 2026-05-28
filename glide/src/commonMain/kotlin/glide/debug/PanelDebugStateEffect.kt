package glide.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Logs panel-local state whenever any tracked key changes.
 * Include selection, filters, form mode, and list counts in [snapshot].
 */
@Composable
fun PanelDebugStateEffect(
    panel: String,
    vararg keys: Any?,
    snapshot: () -> String,
) {
    LaunchedEffect(*keys) {
        GlidePanelDebug.log(panel, "state", snapshot())
    }
}

/** Logs a one-off panel event (save, delete, filter clear, etc.). */
fun panelDebugAction(panel: String, action: String, detail: String = "") {
    GlidePanelDebug.logAction(panel, action, detail)
}
