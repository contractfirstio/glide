package glide.debug

/** Logs a cross-panel state mutation (selection, filter, navigation). */
fun panelStateLog(panel: String, action: String, detail: String = "") {
    GlidePanelDebug.logAction(panel, action, detail)
}
